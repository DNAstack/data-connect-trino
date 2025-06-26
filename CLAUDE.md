# CLAUDE.md - Data Connect Trino

This file provides context and guidelines for AI assistants working with the DNAstack Data Connect Trino repository.


## General directions
- Read @../CLAUDE.md for general context and conventions.

## Repository Overview

Data Connect Trino is an implementation of the GA4GH Discovery Data Connect API on top of Trino (formerly PrestoSQL). This service enables users to enumerate and query data surfaced by a Trino instance in a manner compliant with GA4GH Discovery specifications, returning responses in the Table specification format.

## Key Technologies

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.x
- **Database**: PostgreSQL (for query storage) + Trino (for data queries)
- **Build Tool**: Maven
- **Authentication**: JWT, Basic Auth, or No-Auth profiles
- **API Specification**: GA4GH Discovery Data Connect API
- **Testing**: JUnit 5, Spring Boot Test
- **Containerization**: Docker with multi-stage builds

## Project Structure

```
data-connect-trino/
├── src/main/java/com/dnastack/ga4gh/dataconnect/
│   ├── adapter/
│   │   ├── security/        # Authentication and authorization
│   │   ├── shared/          # Shared utilities and exceptions
│   │   ├── telemetry/       # Telemetry and monitoring
│   │   └── trino/           # Trino-specific adapters and transformers
│   ├── client/              # External service clients
│   │   ├── collectionservice/
│   │   ├── indexingservice/
│   │   └── tablesregistry/
│   ├── controller/          # REST endpoints
│   ├── model/               # Data models
│   └── repository/          # Database repositories
├── e2e-tests/               # End-to-end tests
├── ci/                      # CI/CD scripts and Docker configurations
└── helm/                    # Kubernetes Helm charts
```

## Development Guidelines

### Local Development Setup

1. **Trino Setup**: Ensure you have access to a Trino server
2. **Database Setup**: PostgreSQL must be running with a `dataconnecttrino` database
3. **Environment Variables**: Configure Trino URL and authentication
4. **Run Command**: `mvn clean spring-boot:run`

### Authentication Profiles

The service supports three authentication profiles:

1. **default (JWT)**: Requires JWT tokens with configurable validation
2. **wallet-auth**: DNAstack Wallet-based authentication with policy evaluation
3. **no-auth**: No authentication (development only)
4. **basic-auth**: Basic HTTP authentication

### Code Style and Standards

- Follow DNAstack Java coding standards
- Use Spring Boot conventions for dependency injection
- Implement proper error handling with meaningful error messages
- Document all public APIs with JavaDoc
- Write comprehensive unit and integration tests

### Trino Integration

- Use the Trino client for query execution
- Transform Trino schemas to Data Connect format
- Handle Trino-specific errors gracefully
- Support pagination for large result sets
- Implement query cleanup for resource management

### Database Management

- PostgreSQL is used to store queries for pagination support
- Use Liquibase for schema migrations
- Implement proper connection pooling
- Handle database errors appropriately

## API Development

### Data Connect API Implementation

- Implement GA4GH Discovery Data Connect specification
- Support table enumeration and querying
- Return responses in Table specification format
- Handle CORS appropriately
- Implement proper pagination

### Error Handling

- Map Trino errors to appropriate HTTP status codes
- Provide user-friendly error messages
- Log detailed error information for debugging
- Handle authentication/authorization errors

## Testing

### Unit Tests
- Test business logic in isolation
- Mock external dependencies (Trino, PostgreSQL)
- Test error scenarios
- Verify data transformations

### Integration Tests
- Test Spring Boot configuration
- Verify security configurations
- Test database interactions
- Validate API contracts

### E2E Tests
- Test complete API workflows
- Verify Trino query execution
- Test pagination functionality
- Validate authentication flows

## Configuration

### Key Configuration Properties

```yaml
# Trino configuration
trino.datasource.url: URL of Trino server

# Database configuration
spring.datasource.*: PostgreSQL connection settings

# Authentication configuration
app.auth.*: Authentication and authorization settings

# Service registry configuration
app.tables-registry.*: Tables registry client settings
```

### Environment Variables

- `TRINO_DATASOURCE_URL`: Trino server URL
- `SPRING_PROFILES_ACTIVE`: Active Spring profile
- `APP_AUTH_*`: Authentication configuration
- Database connection variables

## Common Tasks

### Adding New Query Features
1. Update Trino query builder
2. Implement data transformation logic
3. Add appropriate error handling
4. Write unit tests
5. Update API documentation

### Modifying Authentication
1. Update security configuration
2. Modify access evaluator if needed
3. Test with different auth profiles
4. Update deployment documentation

### Integrating New Data Sources
1. Configure Trino catalog/schema
2. Update data model mappings
3. Test query functionality
4. Document new capabilities

## Troubleshooting

### Common Issues
1. **Trino Connection**: Verify Trino URL and network connectivity
2. **Authentication Failures**: Check JWT configuration and token validity
3. **Query Errors**: Review Trino query syntax and permissions
4. **Pagination Issues**: Ensure PostgreSQL is accessible and queries are stored

### Debugging Tips
- Enable debug logging for Trino client
- Check Spring Boot actuator endpoints
- Review Trino query history
- Monitor PostgreSQL query storage

## Performance Considerations

- Implement query result caching where appropriate
- Use connection pooling for database connections
- Monitor Trino query performance
- Implement appropriate timeouts
- Consider result set size limitations

## Security Best Practices

- Validate all input parameters
- Use parameterized queries to prevent SQL injection
- Implement proper authentication and authorization
- Audit sensitive operations
- Follow principle of least privilege for service accounts