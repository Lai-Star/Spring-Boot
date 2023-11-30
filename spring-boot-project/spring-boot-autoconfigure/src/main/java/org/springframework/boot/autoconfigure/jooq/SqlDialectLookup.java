/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jooq;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.JDBCUtils;

/**
 * Utility to lookup well known {@link SQLDialect SQLDialects} from a {@link DataSource}.
 *
 * @author Michael Simons
 * @author Lukas Eder
 * @author Ramil Saetov
 */
final class SqlDialectLookup {

	private static final Log logger = LogFactory.getLog(SqlDialectLookup.class);

	private SqlDialectLookup() {
	}

	/**
	 * Return the most suitable {@link SQLDialect} for the given {@link DataSource}.
	 * @param dataSource the source {@link DataSource}
	 * @return the most suitable {@link SQLDialect}
	 */
	static SQLDialect getDialect(DataSource dataSource) {
		try {
			Connection connection = (dataSource != null) ? dataSource.getConnection() : null;
			return JDBCUtils.dialect(connection);
		}
		catch (SQLException ex) {
			logger.warn("Unable to determine dialect from datasource", ex);
		}
		return SQLDialect.DEFAULT;
	}

}
