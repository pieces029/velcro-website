package com.andrewreitz.velcro.web

import java.io.File
import java.nio.file.Files

import akka.actor.Actor
import net.lingala.zip4j.core._
import net.lingala.zip4j.model._
import net.lingala.zip4j.util._

import scala.sys.process.Process

case class PackageInfo(appName: String, groupId: String, artifactId: String)
case class ZipInfo(file: File, name: String)

object CreateVelcroActor {
  case class Ok(zi: ZipInfo)
  case class Create(pi: PackageInfo)
}

class CreateVelcroActor extends Actor {
  import com.andrewreitz.velcro.web.CreateVelcroActor._

  def receive = {
    case Create(pi) =>
      // Create a temp dir to dump maven output in
      val tempDir = Files.createTempDirectory(null)

      // Attempt to delete on exit
      tempDir.toFile.deleteOnExit()

      // Use maven to generate a new velcro project with the values provided
      Process(s"lazybones create velcro -PpackageName=${pi.groupId} -PapplicationName=${pi.appName} ${pi.appName}", tempDir.toFile).run(connectInput = false).exitValue()

      // Get the generated maven file
      val mavenOutput = tempDir.toFile.listFiles()(0)

      // Zip it up
      val outFile: File = archiveDir(mavenOutput)

      // Send it out
      sender ! Ok(new ZipInfo(outFile, pi.artifactId))
  }

  def archiveDir(input: File): File = {
    // Initiate ZipFile object with the path/name of the zip file.
    val zipFile = new ZipFile(s"${input.getAbsolutePath}.zip")

    // Initiate Zip Parameters which define various properties such
    // as compression method, etc.
    val parameters = new ZipParameters()

    // set compression method to store compression
    parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE)

    // Set the compression level
    parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL)

    // Add folder to the zip file
    zipFile.addFolder(input, parameters)
    zipFile.getFile
  }
}
