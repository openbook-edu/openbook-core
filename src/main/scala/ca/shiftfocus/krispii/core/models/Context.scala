/*
 * Copyright (c) 2014 - ShiftFocus Media Inc.
 * This source code file is part of the Krispii software package
 * and is protected by copyright.
 * All Rights Reserved.
 */

package ca.shiftfocus.krispii.core.models

import ca.shiftfocus.krispii.core.models.course.Course

case class Context(
  user: User,
  session: Session,
  courses: IndexedSeq[Course] = IndexedSeq.empty[Course]
)
