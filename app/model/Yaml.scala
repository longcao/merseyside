package model

import java.util.Date

import org.joda.time.DateTime

import play.api.Logger

import scala.collection.JavaConverters._

object Yaml {
  def empty = Yaml(Map.empty)

  def parseYaml(any: AnyRef): AnyRef = any match {
    case map: java.util.Map[String, AnyRef] => Yaml(map.asScala.toMap.mapValues(parseYaml))
    case list: java.util.List[AnyRef] => list.asScala.toList.map(parseYaml)
    case s: String => s
    case n: Number => n
    case b: java.lang.Boolean => b
    case d: Date => new DateTime(d)
    case null => null
    case other => {
      Logger.warn("Unexpected YAML object of type " + other.getClass)
      other.toString
    }
  }

  def parseFrontMatter(s: String): Yaml = {
    val yaml = new org.yaml.snakeyaml.Yaml().load(s)
    parseYaml(yaml) match {
      case y: Yaml => y
      case other =>
        Logger.warn("YAML didn't parse nicely: " + other)
        Yaml.empty
    }
  }
}

case class Yaml(map: Map[String, AnyRef])
