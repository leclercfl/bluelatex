/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gnieh.blue
package core
package impl
package paper

import http.{
  SyncRoleLet,
  ErrorResponse
}

import common.{
  Logger,
  UserInfo
}

import permission.{
  Role,
  Author,
  Reviewer
}

import couch.{
  Paper,
  PaperRole
}

import com.typesafe.config.Config

import tiscaf.{
  HTalk,
  HStatus
}

import scala.io.Source

import scala.util.Try

import gnieh.sohva.control.CouchClient

/** Returns the paper data
 *
 *  @author Lucas Satabin
 */
class GetPaperInfoLet(paperid: String, val couch: CouchClient, config: Config, logger: Logger) extends SyncRoleLet(paperid, config, logger) {

  def roleAct(user: UserInfo, role: Role)(implicit talk: HTalk): Try[Unit] = role match {
    case Author | Reviewer =>
      // only authenticated users may see other people information
      val manager = entityManager("blue_papers")
      for(paper <- manager.getComponent[Paper](paperid))
        yield paper match {
          // we are sure that the paper has a revision because it comes from the database
          case Some(paper) =>
            talk.writeJson(paper, paper._rev.get)
          case None =>
            talk.setStatus(HStatus.NotFound).writeJson(ErrorResponse("not_found", s"Paper $paperid not found"))
        }

    case _ =>
      Try(
        talk
          .setStatus(HStatus.Forbidden)
          .writeJson(ErrorResponse("no_sufficient_rights", "Only authors or reviewers may see paper information")))
  }

}
