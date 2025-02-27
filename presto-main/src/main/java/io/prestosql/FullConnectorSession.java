/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql;

import com.google.common.collect.ImmutableMap;
import io.prestosql.metadata.SessionPropertyManager;
import io.prestosql.spi.PrestoException;
import io.prestosql.spi.connector.CatalogName;
import io.prestosql.spi.connector.ConnectorSession;
import io.prestosql.spi.security.ConnectorIdentity;
import io.prestosql.spi.type.TimeZoneKey;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.prestosql.spi.StandardErrorCode.INVALID_SESSION_PROPERTY;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class FullConnectorSession
        implements ConnectorSession
{
    private final Session session;
    private final ConnectorIdentity identity;
    private final Map<String, String> properties;
    private final CatalogName catalogName;
    private final String catalog;
    private final SessionPropertyManager sessionPropertyManager;

    public FullConnectorSession(Session session, ConnectorIdentity identity)
    {
        this.session = requireNonNull(session, "session is null");
        this.identity = requireNonNull(identity, "identity is null");
        this.properties = null;
        this.catalogName = null;
        this.catalog = null;
        this.sessionPropertyManager = null;
    }

    public FullConnectorSession(
            Session session,
            ConnectorIdentity identity,
            Map<String, String> properties,
            CatalogName catalogName,
            String catalog,
            SessionPropertyManager sessionPropertyManager)
    {
        this.session = requireNonNull(session, "session is null");
        this.identity = requireNonNull(identity, "identity is null");
        this.properties = ImmutableMap.copyOf(requireNonNull(properties, "properties is null"));
        this.catalogName = requireNonNull(catalogName, "catalogName is null");
        this.catalog = requireNonNull(catalog, "catalog is null");
        this.sessionPropertyManager = requireNonNull(sessionPropertyManager, "sessionPropertyManager is null");
    }

    public Session getSession()
    {
        return session;
    }

    @Override
    public String getQueryId()
    {
        return session.getQueryId().toString();
    }

    @Override
    public Optional<String> getSource()
    {
        return session.getSource();
    }

    @Override
    public ConnectorIdentity getIdentity()
    {
        return identity;
    }

    @Override
    public TimeZoneKey getTimeZoneKey()
    {
        return session.getTimeZoneKey();
    }

    @Override
    public Locale getLocale()
    {
        return session.getLocale();
    }

    @Override
    public long getStartTime()
    {
        return session.getStartTime();
    }

    @Override
    public Optional<String> getTraceToken()
    {
        return session.getTraceToken();
    }

    @Override
    public <T> T getProperty(String propertyName, Class<T> type)
    {
        if (properties == null) {
            throw new PrestoException(INVALID_SESSION_PROPERTY, format("Unknown session property: %s.%s", catalog, propertyName));
        }

        return sessionPropertyManager.decodeCatalogPropertyValue(catalogName, catalog, propertyName, properties.get(propertyName), type);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("queryId", getQueryId())
                .add("user", getUser())
                .add("source", getSource().orElse(null))
                .add("traceToken", getTraceToken().orElse(null))
                .add("timeZoneKey", getTimeZoneKey())
                .add("locale", getLocale())
                .add("startTime", getStartTime())
                .add("properties", properties)
                .omitNullValues()
                .toString();
    }

    // supporting the hive view we need the catalog
    @Override
    public Optional<String> getCatalog()
    {
        return Optional.of(catalog);
    }

    @Override
    public int getTaskWriterCount()
    {
        return SystemSessionProperties.getTaskWriterCount(session);
    }

    @Override
    public boolean isHeuristicIndexFilterEnabled()
    {
        return SystemSessionProperties.isHeuristicIndexFilterEnabled(session);
    }

    @Override
    public boolean isPageMetadataEnabled()
    {
        return session.isPageMetadataEnabled();
    }

    @Override
    public void setPageMetadataEnabled(boolean pageMetadataEnabled)
    {
        session.setPageMetadataEnabled(pageMetadataEnabled);
    }

    @Override
    public boolean isSnapshotEnabled()
    {
        return SystemSessionProperties.isSnapshotEnabled(session);
    }
}
