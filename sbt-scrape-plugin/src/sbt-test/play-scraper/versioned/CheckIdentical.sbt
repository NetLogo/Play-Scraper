import java.io.FilenameFilter
import complete.Parsers.fileParser
import Def.{ inputKey, spaceDelimited }

val checkIdenticalVersioned = inputKey[Unit]("check that two files are identical, assuming that the second's name has been automatically")

val existsVersioned = inputKey[Unit]("check that a file exists, assuming that its name has been automatically fingerprinted")

def shasum(file: File): String =
  Hash.toHex(Hash(file))

def findVersionedFile(file: File): Option[File] =
  file.getParentFile.listFiles(new FilenameFilter {
    override def accept(directory: File, fileName: String): Boolean = {
      fileName.matches(s"[a-f0-9]{32}-${file.getName}")
    }
  }).headOption

checkIdenticalVersioned := {

  val args: Seq[String] = spaceDelimited("<file>").parsed
  val fileA    = file(".") / args(0)
  val fileB    = file(".") / args(1)

  val fileBVersionedOpt = findVersionedFile(fileB)

  fileBVersionedOpt.fold(
    sys.error(s"None of the files in '${fileB.getParent}' (${fileB.getParentFile.list.mkString("'", "' and '", "'")}) have the form '<32 hex digits>-${fileB.getName}'")
  )(
    fileBVersioned =>
      if (shasum(fileA) != shasum(fileBVersioned)) {
        sys.error(s"expected $fileA (${shasum(fileA)}) to match $fileBVersioned (${shasum(fileBVersioned)}) but they did not match")
      }
  )

}

existsVersioned := {
  val f = fileParser(file(".")).parsed
  if (findVersionedFile(f).isEmpty) {
    sys.error(s"None of the files in '${f.getParent}' (${f.getParentFile.list.mkString("'", "' and '", "'")}) have the form '<32 hex digits>-${f.getName}'")
  }
}
