package com.youdevise.lofty

import scala.language.dynamics
import scalaz._
import Scalaz._

object Builders {

  type Transcripts = Map[BuildTarget[_], Seq[RecordedCall]]

  trait Builder[T] {
    def buildFrom(properties: Map[String, _]): T
  }

  private[this] val emptyRecording = Recording(Seq.empty[RecordedCall])

  def builder[T](implicit builder: Builder[T]) = state[Recording, BuildTarget[T]] { (recording: Recording) =>
    (recording, new BuildTarget[T](builder))
  }

  private[this] def transcriptsFor[A](state: State[Recording, A]): (Transcripts, A) = {
    val (recording, target) = state(emptyRecording)
    val transcripts = recording.calls.groupBy(_.target)
    (transcripts, target)
  }

  def buildFrom[T](state: State[Recording, Buildable[T]]): T = {
    val (transcripts, target) = transcriptsFor(state)
    target.buildFrom(transcripts)
  }

  def buildFrom[T1, T2](state: State[Recording, (Buildable[T1], Buildable[T2])]): (T1, T2) = {
    val (transcripts, (target1, target2)) = transcriptsFor(state)
    (target1.buildFrom(transcripts), target2.buildFrom(transcripts))
  }

  def buildFrom[T1, T2, T3](state: State[Recording, (Buildable[T1], Buildable[T2], Buildable[T3])]): (T1, T2, T3) = {
    val (transcripts, (target1, target2, target3)) = transcriptsFor(state)
    (target1.buildFrom(transcripts), target2.buildFrom(transcripts), target3.buildFrom(transcripts))
  }
}

import Builders._

sealed case class RecordedCall(target: BuildTarget[_], methodName: String, parameters: Seq[Any]) {
  lazy val property = methodName -> parameters.head
}

sealed case class Recording(val calls: Seq[RecordedCall])

trait Buildable[T] {
  protected def reify(transcripts: Transcripts, value: Any): Any = value match {
    case target: BuildTarget[_] => target.buildFrom(transcripts)
    case targets: Iterable[_] => targets.map(reify(transcripts, _))
    case _ => value
  }

  def buildFrom(transcripts: Transcripts): T
}

sealed class BuildTarget[T](val builder: Builder[T]) extends Buildable[T] with Dynamic {
  private def record(name: String, args: Any*) = {
    val target = this
    state[Recording, BuildTarget[T]] { (recording:Recording) => (
      new Recording(recording.calls :+ RecordedCall(target, name, args.toSeq)),
      target)
    }
  }

  def applyDynamic(name: String)(args: Any*) = record(name, args:_*)

  override def buildFrom(transcripts: Transcripts): T = {
    val myTranscript = transcripts(this)
    val properties = myTranscript.map(_.property).toMap
    val reifiedProperties = properties.mapValues(reify(transcripts, _))
    builder.buildFrom(reifiedProperties)
  }
}
