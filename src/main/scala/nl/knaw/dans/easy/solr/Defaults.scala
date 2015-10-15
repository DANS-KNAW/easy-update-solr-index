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
