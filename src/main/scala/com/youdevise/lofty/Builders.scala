package com.youdevise.lofty

import scala.language.dynamics
import scalaz._
import Scalaz._

object Builders {

  trait Builder[T] {
    def buildFrom(properties: Map[String, _]): T
  }

  private val emptyRecording = Recording(Seq.empty[RecordedCall])

  def builder[T](implicit builder: Builder[T]) = state[Recording, BuildTarget[T]] { (recording: Recording) =>
    (recording, new BuildTarget[T](builder))
  }

  def buildFrom[T](state: State[Recording, BuildTarget[T]]): T = {
    val (recording, target) = state(emptyRecording)
    val transcripts = recording.calls.groupBy(_.target)
    target.buildFrom(transcripts)
  }
}

import Builders._

sealed case class RecordedCall(target: BuildTarget[_], methodName: String, parameters: Seq[Any]) {
  lazy val property = methodName -> parameters.head
}

sealed case class Recording(val calls: Seq[RecordedCall])

sealed class BuildTarget[T](val builder: Builder[T]) extends Dynamic {
  private def record(name: String, args: Any*) = {
    val target = this
    state[Recording, BuildTarget[T]] { (recording:Recording) => (
      new Recording(recording.calls :+ RecordedCall(target, name, args.toSeq)),
      target)
    }
  }

  private[this] def reify(transcripts: Map[BuildTarget[_], Seq[RecordedCall]], value: Any): Any = value match {
    case target: BuildTarget[_] => target.buildFrom(transcripts)
    case targets: Iterable[_] => targets.map(reify(transcripts, _))
    case _ => value
  }

  def applyDynamic(name: String)(args: Any*) = record(name, args:_*)

  def buildFrom(transcripts: Map[BuildTarget[_], Seq[RecordedCall]]): T = {
    val myTranscript = transcripts(this)
    val properties = myTranscript.map(_.property).toMap
    val reifiedProperties = properties.mapValues(reify(transcripts, _))
    builder.buildFrom(reifiedProperties)
  }
}
