/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import org.rogach.scallop.{ScallopConf, ScallopOption, Subcommand, singleArgConverter}

class CommandLineOptions(args: Array[String], configuration: Configuration) extends ScallopConf(args) {
  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))
  printedName = "easy-update-solr-index"
  private val SUBCOMMAND_SEPARATOR = "---\n"
  val description: String = s"""An example module generated with easy-module-archetype stripped down to only command line interface."""
  val synopsis: String =
    s"""
       |  $printedName (synopsis of command line parameters)
       |  $printedName (... possibly multiple lines for subcommands)""".stripMargin

  version(s"$printedName v${ configuration.version }")
  banner(
    s"""
       |  $description
       |
       |Usage:
       |
       |$synopsis
       |
       |Options:
       |""".stripMargin)
  val url: ScallopOption[String] = opt("someOption", noshort = true, descr = "Description of the option")

  footer("")
}
