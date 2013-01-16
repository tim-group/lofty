package com.youdevise.lofty

import scala.language.dynamics
import scalaz._
import Scalaz._

object Builders {

  type Recording = Seq[RecordedCall]
  type RecorderState[R] = State[Recording, R]
  type Transcripts = Map[Buildable[_], Seq[RecordedCall]]

  def recordingState[T](f: Recording => (Recording, T)) = state[Recording, T](f)

  trait Builder[T] {
    def buildFrom(properties: Map[String, _]): T
  }

  private[this] val emptyRecording = Seq.empty[RecordedCall]

  def builder[T](implicit builder: Builder[T]) = recordingState[DynamicBuildable[T]] { (recording: Recording) =>
    (recording, new DynamicBuildable[T](builder))
  }

  private[this] def transcriptsFor[A](state: RecorderState[A]): (Transcripts, A) = {
    val (recording, target) = state(emptyRecording)
    val transcripts = recording.groupBy(_.target)
    (transcripts, target)
  }

  def buildFrom[T](state: RecorderState[Buildable[T]]): T = {
    val (transcripts, target) = transcriptsFor(state)
    target.buildFrom(transcripts)
  }

  def buildFrom[T1, T2](state: RecorderState[(Buildable[T1], Buildable[T2])]): (T1, T2) = {
    val (transcripts, (target1, target2)) = transcriptsFor(state)
    (target1.buildFrom(transcripts), target2.buildFrom(transcripts))
  }

  def buildFrom[T1, T2, T3](state: RecorderState[(Buildable[T1], Buildable[T2], Buildable[T3])]): (T1, T2, T3) = {
    val (transcripts, (target1, target2, target3)) = transcriptsFor(state)
    (target1.buildFrom(transcripts), target2.buildFrom(transcripts), target3.buildFrom(transcripts))
  }
}

import Builders._

sealed case class RecordedCall(target: Buildable[_], methodName: String, parameters: Seq[Any]) {
  lazy val property = methodName -> parameters.head
}

trait Buildable[T] {
  protected def reify(transcripts: Transcripts, value: Any): Any = value match {
    case target: Buildable[_] => target.buildFrom(transcripts)
    case targets: Iterable[_] => targets.map(reify(transcripts, _))
    case _ => value
  }

  def buildFrom(transcripts: Transcripts): T
}

sealed class DynamicBuildable[T](val builder: Builder[T]) extends Buildable[T] with Dynamic {

  def applyDynamic(name: String)(args: Any*) = {
    val target = this
    recordingState[DynamicBuildable[T]] { (recording:Recording) => (
      recording :+ RecordedCall(target, name, args.toSeq),
      target)
    }
  }

  def selectDynamic(name: String) = {
    val target = this
    new Buildable[Any] {
      def buildFrom(transcripts: Transcripts): Any = {
        val properties = transcripts(target).map(_.property).toMap
        properties(name)
      }
    }
  }

  override def buildFrom(transcripts: Transcripts): T = {
    val properties = transcripts(this).map(_.property).toMap
    val reifiedProperties = properties.mapValues(reify(transcripts, _))
    builder.buildFrom(reifiedProperties)
  }
}
