/*
 * Copyright (c) 2014 - ShiftFocus Media Inc.
 * This source code file is part of the Krispii software package
 * and is protected by copyright.
 * All Rights Reserved.
 */

package com.shiftfocus.krispii.common.models

case class Context(
  user: User,
  session: Session,
  roles: IndexedSeq[String],
  sections: IndexedSeq[Section]
)
