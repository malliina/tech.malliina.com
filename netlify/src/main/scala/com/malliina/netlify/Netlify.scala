package com.malliina.netlify

import cats.effect.{Async, Concurrent}
import cats.implicits.{toFlatMapOps, toFunctorOps, toTraverseOps}
import com.malliina.http.io.{HttpClientF2, HttpClientIO}
import com.malliina.http.{FullUrl, OkHttpResponse}
import com.malliina.netlify.Netlify._
import com.malliina.util.AppLogger
import com.malliina.values.{AccessToken, StringCompanion, WrappedString}
import fs2.hashing.{HashAlgorithm, Hashing}
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import okhttp3.{MediaType, RequestBody}

import scala.language.higherKinds

/** @see
  *   https://docs.netlify.com/api/get-started/#file-digest-method
  */
object Netlify {
  val octetStream = MediaType.parse("application/octet-stream")

  private val log = AppLogger(getClass)

  case class SiteId(value: String) extends AnyVal with WrappedString
  object SiteId extends StringCompanion[SiteId]

  case class SHA1(value: String) extends AnyVal with WrappedString
  object SHA1 extends StringCompanion[SHA1]

  case class SHA256(value: String) extends AnyVal with WrappedString
  object SHA256 extends StringCompanion[SHA256]

  case class FileDigests(files: Map[String, SHA1], functions: Map[String, SHA256])
  object FileDigests {
    implicit val json: Codec[FileDigests] = deriveCodec[FileDigests]

    def from(map: Map[Path, SHA1], root: Path): FileDigests = {
      val paths = map.map { case (path, sha1) =>
        val rel = root.relativize(path).toString
        s"/$rel" -> sha1
      }
      FileDigests(paths, Map.empty)
    }
  }

  case class DeployId(value: String) extends AnyVal with WrappedString
  object DeployId extends StringCompanion[DeployId]

  case class Uploadables(
    id: DeployId,
    required: Option[Seq[SHA1]],
    required_functions: Option[Seq[SHA256]]
  ) {
    def filesList: List[SHA1] = required.toList.flatMap(_.toList)
  }
  object Uploadables {
    implicit val json: Codec[Uploadables] = deriveCodec[Uploadables]
  }

  case class DeployResult(id: DeployId)

  def deploy[F[_]: Async: Files: Concurrent: Hashing](
    root: Path,
    site: SiteId,
    token: AccessToken
  ): F[DeployId] =
    HttpClientIO.resource[F].use { http =>
      val client = new Netlify(token, http)
      client.deploy(root, site)
    }

  def computeDigests[F[_]: Async: Files: Concurrent: Hashing](root: Path): F[Map[Path, SHA1]] = {
    val F = Files[F]
    val H = Hashing[F]
    H.hash(HashAlgorithm.SHA1)
    listRecursively(root).flatMap { file =>
      F.readAll(file)
        .through(H.hash(HashAlgorithm.SHA1))
        .map(_.bytes)
        .unchunks
        .through(text.hex.encode)
        .map(sha1 => file -> SHA1(sha1))
    }
  }.compile.toList
    .map(_.toMap)

  def listRecursively[F[_]: Async: Files](root: Path): Stream[F, Path] = {
    val F = Files[F]
    F.list(root).flatMap { p =>
      Stream.eval(F.isRegularFile(p)).flatMap { isRegular =>
        if (isRegular) {
          Stream.emit[F, Path](p)
        } else {
          listRecursively(p)
        }
      }
    }
  }
}

class Netlify[F[_]: Async: Files: Concurrent: Hashing](token: AccessToken, http: HttpClientF2[F]) {
  val F = Async[F]
  val baseUrl = FullUrl.https("api.netlify.com", "/api/v1")
  private val headers = Map(
    "Authorization" -> s"Bearer $token",
    "User-Agent" -> "SkogbergLabs (info@skogberglabs.com)"
  )

  /** Deploys all files under `root` to Netlify.
    *
    * @param root
    *   site root
    * @param to
    *   site identifier
    */
  def deploy(root: Path, to: SiteId): F[DeployId] =
    for {
      ds <- computeDigests(root)
      digests = FileDigests.from(ds, root)
      us <- uploadables(digests, to)
      rs <- us.filesList.traverse[F, OkHttpResponse] { sha1 =>
        val maybeUpload = for {
          path <- ds.find(_._2 == sha1).map(_._1).toRight(s"Path not found for '$sha1'.")
          pair <- digests.files.find(_._2 == sha1).toRight(s"Digest not found for '$sha1'.")
        } yield uploadFile(pair._1, us.id, path)
        maybeUpload.fold(err => F.raiseError(new Exception(err)), identity)
      }
    } yield us.id

  def uploadables(in: FileDigests, site: SiteId): F[Uploadables] =
    http
      .postAs[FileDigests, Uploadables](baseUrl / "sites" / site.value / "deploys", in, headers)
      .flatTap { us =>
        val msg = s"Need to upload ${us.filesList.size} required files for deployment ${us.id}."
        F.delay(log.info(msg))
      }

  def uploadFile(path: String, deployId: DeployId, file: Path) = {
    val body = RequestBody.create(file.toNioPath.toFile, octetStream)
    val url = baseUrl / "deploys" / deployId.value / "files" / path
    http.put(url, body, headers).flatTap { res =>
      Files[F].size(file).map { bytes =>
        val msg = s"Uploaded $bytes bytes from $file to '$url' status ${res.code}."
        F.delay(log.info(msg))
      }
    }
  }
}
