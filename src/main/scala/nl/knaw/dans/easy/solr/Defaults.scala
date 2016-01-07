/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.solr

import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf
import scala.collection.JavaConverters._

object Defaults {

  /**
   * Gets defaults from props for conf.options that are not in args.
   *
   * @param props key-value pairs: if a key is prefixed with "default."
   *              the rest of the key should equal one of the option names
   * @param conf a validating instance
   * @param args command line arguments
   * @return key-value pairs from props for conf.options not in args
   */
  def filterDefaultOptions(props: PropertiesConfiguration, conf: ScallopConf, args: Seq[String]): Seq[String] = {

    /** maps long option names to the explicitly defined short keys */
    val keyMap = conf.builder.opts
      .withFilter(opt => opt.requiredShortNames.nonEmpty)
      .map(opt => (opt.name, opt.requiredShortNames.head)).toMap

    val longArgs = args.filter(arg => arg.matches("--.*")).map(arg => arg.replaceFirst("--",""))
    val shortArgs = args.filter(arg => arg.matches("-[^-].*")).map(arg => arg.charAt(1))

    def keyValuePair(key: String): Array[String] =
      Array (s"--${key.replace("default.", "")}", props.getString(key))

    def inArgs(key: String): Boolean =
      longArgs.contains(key) || shortArgs.contains(keyMap.getOrElse(key,null))

    if (args.contains("--help") || args.contains("--version")) Array[String]()
    else props.getKeys.asScala
      .withFilter(key => key.startsWith("default.") && !inArgs(key.replace("default.","")))
      .toArray.flatMap(key => keyValuePair(key))
  }
}
