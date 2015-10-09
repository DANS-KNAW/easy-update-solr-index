package nl.knaw.dans.easy.solr

import org.apache.commons.configuration.PropertiesConfiguration
import scala.collection.JavaConverters._

object Defaults {

  /** maps long option names to the explicitly defined short keys */
  val keyMap = new Conf().builder.opts
    .withFilter(opt => opt.requiredShortNames.nonEmpty)
    .map(opt => (opt.name, opt.requiredShortNames.head)).toMap

  /**
   * Gets defaults from properties for options that are not on the command line.
   *
   * @param args command line arguments
   * @param props key-value pairs: if a key is prefixed with "default."
   *              the rest of the key should equal one of the option names
   * @return key-value pairs from props for keys not in args
   */
  def filter(args: Seq[String], props: PropertiesConfiguration): Seq[String] = {

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
