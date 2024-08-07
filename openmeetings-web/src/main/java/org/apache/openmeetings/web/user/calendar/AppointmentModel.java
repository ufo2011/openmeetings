/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.user.calendar;

import static org.apache.openmeetings.web.app.Application.get;
import static org.apache.openmeetings.web.app.WebSession.getUserId;
import static org.apache.openmeetings.web.util.CalendarWebHelper.getDate;

import java.util.ArrayList;
import java.util.List;

import org.apache.openmeetings.db.dao.calendar.AppointmentDao;
import org.apache.openmeetings.db.entity.calendar.Appointment;

import org.wicketstuff.jquery.ui.calendar6.CalendarEvent;
import org.wicketstuff.jquery.ui.calendar6.CalendarModel;
import org.wicketstuff.jquery.ui.calendar6.ICalendarVisitor;

public class AppointmentModel extends CalendarModel implements ICalendarVisitor {
	private static final long serialVersionUID = 1L;

	@Override
	public void visit(CalendarEvent event) {
		//every event can be customized
	}

	@Override
	protected List<? extends CalendarEvent> load() {
		List<CalendarEvent> list = new ArrayList<>();
		for (Appointment a : get().getBean(AppointmentDao.class).getInRange(getUserId(), getDate(getStart()), getDate(getEnd()))) {
			list.add(new OmCalendarEvent(a));
		}
		return list;
	}
}
