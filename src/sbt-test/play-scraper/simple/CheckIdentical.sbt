import Def.{ inputKey, spaceDelimited }

val checkIdentical = inputKey[Unit]("check that two files are identical")

def shasum(file: File): String =
  Hash.toHex(Hash(file))

checkIdentical := {
  val args: Seq[String] = spaceDelimited("<file>").parsed
  val fileA = file(".") / args(0)
  val fileB = file(".") / args(1)

  if (shasum(fileA) != shasum(fileB)) {
    sys.error("expected " + fileA.toString + " (" + shasum(fileA) + ")  to match " +
      fileB.toString + " (" + shasum(fileB) + ") but they did not match")
  }
}
