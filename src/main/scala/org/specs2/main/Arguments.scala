package org.specs2
package main

import org.specs2.internal.scalaz.{ Monoid, Scalaz }
import Scalaz._
import control._
import Exceptions._
import text.{AnsiColors, DiffShortener, EditDistance}
import reporter.{SmartColors, Colors}

/**
 * This class holds all the options that are relevant for specs2 execution and reporting.
 */
private[specs2]  
case class Arguments (
  select:        Select           = Select(),
  execute:       Execute          = Execute(),
  report:        Report           = Report(),
  commandLine:   Seq[String]      = Nil
 ) extends ShowArgs {
  def ex: String                    = select.ex
  def include: String               = select.include
  def exclude: String               = select.exclude
  def specName: String              = select.specName
  def plan: Boolean                 = execute.plan
  def skipAll: Boolean              = execute.skipAll
  def stopOnFail: Boolean           = execute.stopOnFail
  def sequential: Boolean           = execute.sequential
  def threadsNb: Int                = execute.threadsNb
  def xonly: Boolean                = report.xonly
  def failtrace: Boolean            = report.failtrace
  def color: Boolean                = report.color
  def colors: Colors                = report.colors
  def noindent: Boolean             = report.noindent
  def showtimes: Boolean            = report.showtimes
  def offset: Int                   = report.offset
  def markdown: Boolean             = report.markdown
  def debugMarkdown: Boolean        = report.debugMarkdown
  def diffs: Diffs                  = report.diffs
  def fromSource: Boolean           = report.fromSource
  def traceFilter: StackTraceFilter = report.traceFilter

  /** @return true if the command line contains a given string */
  def contains(a: String) = commandLine contains a
  /** alias for overrideWith */
  def <|(other: Arguments) = overrideWith(other)
  
  /**
   * @return a new Arguments object where the values of this are overriden with the values of other if defined
   */
  def overrideWith(other: Arguments): Arguments = {
    new Arguments(
      select.overrideWith(other.select),
      execute.overrideWith(other.execute),
      report.overrideWith(other.report),
      if (other.commandLine.isEmpty) commandLine else other.commandLine
    )
  }

  def textColor   (s: String) = colors.text   (s, color)
  def successColor(s: String) = colors.success(s, color)
  def failureColor(s: String) = colors.failure(s, color)
  def errorColor  (s: String) = colors.error  (s, color)
  def pendingColor(s: String) = colors.pending(s, color)
  def skippedColor(s: String) = colors.skipped(s, color)
  def statsColor  (s: String) = colors.stats  (s, color)
  def removeColors(s: String) = colors.removeColors(s)

  override def toString =
    (List(select.toString,
         execute.toString,
         report.toString) ++
         (if (commandLine.isEmpty) Nil else List("commandLine = "+commandLine.mkString(", ")))).mkString("Arguments(", ", ", ")")

}
import main.{SystemProperties => sysProperties}

private[specs2]  
object Arguments extends Extract {
  
  /** @return new arguments from command-line arguments */
  def apply(implicit arguments: String*): Arguments = {
    extract(arguments, sysProperties)
  }
  private[specs2] def extract(implicit arguments: Seq[String], systemProperties: SystemProperties): Arguments = {
    new Arguments (
       select        = Select.extract,
       execute       = Execute.extract,
       report        = Report.extract,
       commandLine   = arguments
    )
  }
  
  implicit def ArgumentsMonoid: Monoid[Arguments] = new Monoid[Arguments] {
    def append(a1: Arguments, a2: =>Arguments) = a1 overrideWith a2
    val zero = Arguments()
  }
}

case class Select(
  _ex:            Option[String]           = None,
  _include:       Option[String]           = None,
  _exclude:       Option[String]           = None,
  _failedOnly:    Option[Boolean]          = None,
  _specName:      Option[String]           = None) extends ShowArgs {

  def ex: String                    = _ex.getOrElse(".*")
  def include: String               = _include.getOrElse("")
  def exclude: String               = _exclude.getOrElse("")
  def failedOnly: Boolean           = _failedOnly.getOrElse(false)
  def specName: String              = _specName.getOrElse(".*Spec")

  def overrideWith(other: Select) = {
    new Select(
      other._ex              .orElse(_ex),
      other._include         .orElse(_include),
      other._exclude         .orElse(_exclude),
      other._failedOnly      .orElse(_failedOnly),
      other._specName        .orElse(_specName)
    )
  }

  override def toString = List(
    "ex"             -> _ex           ,
    "include"        -> _include      ,
    "exclude"        -> _exclude      ,
    "failedOnly"     -> _failedOnly   ,
    "specName"       -> _specName     ).flatMap(showArg).mkString("Select(", ", ", ")")

}

private[specs2]
object Select extends Extract {
  def extract(implicit arguments: Seq[String], systemProperties: SystemProperties): Select = {
    new Select (
       _ex            = value("ex", ".*"+(_:String)+".*"),
       _include       = value("include"),
       _exclude       = value("exclude"),
       _failedOnly    = bool("failedonly"),
       _specName      = value("specname")
    )
  }
}

private[specs2]
case class Execute(
  _plan:          Option[Boolean]          = None,
  _skipAll:       Option[Boolean]          = None,
  _stopOnFail:    Option[Boolean]          = None,
  _sequential:    Option[Boolean]          = None,
  _threadsNb:     Option[Int]              = None) extends ShowArgs {

  def plan: Boolean                 = _plan.getOrElse(false)
  def skipAll: Boolean              = _skipAll.getOrElse(false)
  def stopOnFail: Boolean           = _stopOnFail.getOrElse(false)
  def sequential: Boolean           = _sequential.getOrElse(false)
  def threadsNb: Int                = _threadsNb.getOrElse(Runtime.getRuntime.availableProcessors)

  def overrideWith(other: Execute) = {
    new Execute(
      other._plan            .orElse(_plan),
      other._skipAll         .orElse(_skipAll),
      other._stopOnFail      .orElse(_stopOnFail),
      other._sequential      .orElse(_sequential),
      other._threadsNb       .orElse(_threadsNb)
    )
  }

  override def toString =
    List(
    "plan"           -> _plan         ,
    "skipAll"        -> _skipAll      ,
    "stopOnFail"     -> _stopOnFail   ,
    "sequential"     -> _sequential   ,
    "threadsNb"      -> _threadsNb    ).flatMap(showArg).mkString("Execute(", ", ", ")")

}
private[specs2]
object Execute extends Extract {
  def extract(implicit arguments: Seq[String], systemProperties: SystemProperties): Execute = {
    new Execute (
      _plan          = bool("plan"),
      _skipAll       = bool("skipall"),
      _stopOnFail    = bool("stoponfail"),
      _sequential    = bool("sequential"),
      _threadsNb     = int("threadsnb")
    )
  }
}

private[specs2]
case class Report(
  _xonly:         Option[Boolean]          = None,
  _failtrace:     Option[Boolean]          = None,
  _color:         Option[Boolean]          = None,
  _colors:        Option[Colors]           = None,
  _noindent:      Option[Boolean]          = None,
  _showtimes:     Option[Boolean]          = None,
  _offset:        Option[Int]              = None,
  _markdown:      Option[Boolean]          = None,
  _debugMarkdown: Option[Boolean]          = None,
  _diffs:         Option[Diffs]            = None,
  _fromSource:    Option[Boolean]          = None,
  _traceFilter:   Option[StackTraceFilter] = None) extends ShowArgs {

  def xonly: Boolean                = _xonly.getOrElse(false)
  def failtrace: Boolean            = _failtrace.getOrElse(false)
  def color: Boolean                = _color.getOrElse(true)
  def colors: Colors                = _colors.getOrElse(new SmartColors())
  def noindent: Boolean             = _noindent.getOrElse(false)
  def showtimes: Boolean            = _showtimes.getOrElse(false)
  def offset: Int                   = _offset.getOrElse(0)
  def markdown: Boolean             = _markdown.getOrElse(true)
  def debugMarkdown: Boolean        = _debugMarkdown.getOrElse(false)
  def diffs: Diffs                  = _diffs.getOrElse(SmartDiffs())
  def fromSource: Boolean           = _fromSource.getOrElse(true)
  def traceFilter: StackTraceFilter = _traceFilter.getOrElse(DefaultStackTraceFilter)

  def overrideWith(other: Report) = {
    new Report(
      other._xonly           .orElse(_xonly),
      other._failtrace       .orElse(_failtrace),
      other._color           .orElse(_color),
      other._colors          .orElse(_colors),
      other._noindent        .orElse(_noindent),
      other._showtimes       .orElse(_showtimes),
      other._offset          .orElse(_offset),
      other._markdown        .orElse(_markdown),
      other._debugMarkdown   .orElse(_debugMarkdown),
      other._diffs           .orElse(_diffs),
      other._fromSource      .orElse(_fromSource),
      other._traceFilter     .orElse(_traceFilter)
    )
  }

  override def toString = List(
    "xonly"          -> _xonly        ,
    "failtrace"      -> _failtrace    ,
    "color"          -> _color        ,
    "colors"         -> _colors       ,
    "noindent"       -> _noindent     ,
    "showtimes"      -> _showtimes    ,
    "offset"         -> _offset       ,
    "markdown"       -> _markdown     ,
    "debugMarkdown"  -> _debugMarkdown,
    "diffs"          -> _diffs,
    "fromSource"     -> _fromSource,
    "traceFilter"    -> _traceFilter).flatMap(showArg).mkString("Report(", ", ", ")")

}
private[specs2]
object Report extends Extract {
  def extract(implicit arguments: Seq[String], systemProperties: SystemProperties): Report = {
    new Report (
      _xonly         = bool("xonly"),
      _failtrace     = bool("failtrace"),
      _color         = bool("color", "nocolor"),
      _colors        = value("colors").map(SmartColors.fromArgs).orElse(Some(new SmartColors)),
      _noindent      = bool("noindent"),
      _showtimes     = bool("showtimes"),
      _offset        = int("offset"),
      _markdown      = bool("markdown", "nomarkdown"),
      _debugMarkdown = bool("debugmarkdown"),
      _fromSource    = bool("fromsource"),
      _traceFilter   = bool("fullstacktrace").map(t=>NoStackTraceFilter).
                       orElse(value("tracefilter", IncludeExcludeStackTraceFilter.fromString(_)))
    )
  }
}

private[specs2]
trait Extract {
  def bool(name: String, mappedValue: Boolean = true)(implicit args: Seq[String], sp: SystemProperties): Option[Boolean] = {
    args.find(_.toLowerCase.contains(name.toLowerCase)).map(a => mappedValue).orElse(boolSystemProperty(name))
  }
  def boolSystemProperty(name: String)(implicit sp: SystemProperties): Option[Boolean] = {
    sp.getPropertyAs[Boolean](name) orElse sp.getProperty(name).map(v => true)
  }
  def bool(name: String, negatedName: String)(implicit args: Seq[String], sp: SystemProperties): Option[Boolean] = {
    bool(negatedName, false) orElse bool(name)
  }
  def value[T](name: String, f: String => T)(implicit args: Seq[String], sp: SystemProperties): Option[T] = {
    args.zip(args.drop(1)).find(_._1.toLowerCase.equals(name.toLowerCase)).map(s => f(s._2)).orElse(valueSystemProperty(name, f))
  }
  def valueSystemProperty[T](name: String, f: String => T)(implicit sp: SystemProperties): Option[T] = {
    sp.getProperty(name).map(o => f(o.toString))
  }
  def value[T](name: String)(implicit args: Seq[String], sp: SystemProperties): Option[String] = value(name, identity _)
  def int(name: String)(implicit args: Seq[String], sp: SystemProperties): Option[Int] = {
    tryo(value(name)(args, sp).map(_.toInt).get)
  }

}

/**
 * this trait is used to define and compute the differences between strings (used by the reporters)
 */
trait Diffs {
  /** @return true if the differences must be shown */
  def show: Boolean
  /** @return true if the differences must be shown for 2 different strings */
  def show(expected: String, actual: String): Boolean
  /** @return the diffs */
  def showDiffs(expected: String, actual: String): (String, String)
  /** @return true if the full strings must also be shown */
  def showFull: Boolean
  /** @return the separators to use*/
  def separators: String
}

/**
 * The SmartDiffs class holds all the required parameters to show differences between 2 strings using the edit distance
 * algorithm
 */
case class SmartDiffs(show: Boolean = true, separators: String = "[]", triggerSize: Int = 20, shortenSize: Int = 5, diffRatio: Int = 30, showFull: Boolean = false) extends Diffs {
  import EditDistance._

  def show(expected: String, actual: String): Boolean = show && Seq(expected, actual).exists(_.size >= triggerSize)
  def showDiffs(expected: String, actual: String) = {
    if (editDistance(expected, actual).doubleValue / (expected.size + actual.size) < diffRatio.doubleValue / 100)
      showDistance(expected, actual, separators, shortenSize)
    else
      (expected, actual)
  }
}

private[specs2]
trait ShowArgs {
  def showArg(a: (String, Option[_])) = a._2.map(a._1 +" = "+_)
}