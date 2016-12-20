package com.gh.helper.domain

import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import slick.model.ForeignKeyAction
import java.sql.Timestamp

object PermissionType extends Enumeration {
  type PermissionType = Value
  val God = Value("God")
  val Admin = Value("Admin")
  val Normal = Value("Normal")
  val Nothing = Value("Nothing")
}


/**
 * Customer entity.
 *
 * @param id        unique id
 * @param firstName first name
 * @param lastName  last name
 * @param fbToken   facebook access token
 */
case class Permission(id : Option[Long], userId: String, permission: PermissionType.Value, createDate: Option[java.sql.Timestamp])


/**
 * Mapped customers table object.
 */
class Permissions(tag: Tag) extends Table[Permission](tag, "permissions") {

  implicit val permissionTypeMapper = MappedColumnType.base[PermissionType.Value, String](
	{ pt => pt.toString },
	{ st => PermissionType.withName(st)	})

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def userId = column[String]("userId", O.Length(64))

  def permission = column[PermissionType.Value]("permission", O.Length(64))

  def createDate = column[java.sql.Timestamp]("createDate")

  def * = (id.?, userId, permission, createDate.?) <> (Permission.tupled, Permission.unapply)

  def user = foreignKey("user_permission_FK", userId, TableQuery[Users])(_.fbId, onUpdate=ForeignKeyAction.Restrict)

}