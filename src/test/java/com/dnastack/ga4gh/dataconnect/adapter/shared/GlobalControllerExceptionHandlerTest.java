package com.dnastack.ga4gh.dataconnect.adapter.shared;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.InvalidQueryJobException;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.MalformedClientSuppliedCredentialsException;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TableApiErrorException;
import com.dnastack.ga4gh.dataconnect.model.TableData;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * How a request this service will not serve is reported to the operator. A caller's own mistake is not this
 * service's error, so it must not arrive in the logs looking like one: an error dashboard that counts malformed
 * headers alongside genuine failures cannot be used to tell whether the service is healthy.
 */
public class GlobalControllerExceptionHandlerTest {

    private final ListAppender<ILoggingEvent> loggedEvents = new ListAppender<>();
    private final Logger handlerLogger = (Logger) LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    @Before
    public void captureTheHandlersLog() {
        loggedEvents.list = new CopyOnWriteArrayList<>();
        loggedEvents.start();
        handlerLogger.addAppender(loggedEvents);
    }

    @After
    public void releaseTheHandlersLog() {
        handlerLogger.detachAppender(loggedEvents);
        loggedEvents.stop();
    }

    private GlobalControllerExceptionHandler handler() {
        TraceContext traceContext = mock(TraceContext.class);
        when(traceContext.traceId()).thenReturn("0af7651916cd43dd8448eb211c80319c");
        Span span = mock(Span.class);
        when(span.context()).thenReturn(traceContext);
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);

        GlobalControllerExceptionHandler handler = new GlobalControllerExceptionHandler();
        ReflectionTestUtils.setField(handler, "tracer", tracer);
        return handler;
    }

    private ILoggingEvent theOnlyLoggedEvent() {
        List<ILoggingEvent> events = loggedEvents.list;
        assertThat(events).as("the events the handler logged").hasSize(1);
        return events.getFirst();
    }

    private void handle(Throwable cause) {
        handler().handleTableApiErrorException(new TableApiErrorException(cause, TableData::errorInstance));
    }

    @Test
    public void handleTableApiErrorException_should_logAClientErrorAtInfo() {
        handle(new MalformedClientSuppliedCredentialsException("not of the form name=value"));

        assertThat(theOnlyLoggedEvent().getLevel())
            .as("the level a 400 is reported at")
            .isEqualTo(Level.INFO);
    }

    @Test
    public void handleTableApiErrorException_shouldNot_logAStackTraceForAClientError() {
        // A caller's mistake has no stack trace worth reading: the fault is in the request, not in this code.
        handle(new MalformedClientSuppliedCredentialsException("not of the form name=value"));

        assertThat(theOnlyLoggedEvent().getThrowableProxy())
            .as("the throwable attached to a 400")
            .isNull();
    }

    @Test
    public void handleTableApiErrorException_should_sayWhatTheClientErrorWas() {
        handle(new MalformedClientSuppliedCredentialsException("not of the form name=value"));

        assertThat(theOnlyLoggedEvent().getFormattedMessage())
            .as("what a 400 is reported as")
            .contains("400")
            .contains("not of the form name=value");
    }

    @Test
    public void handleTableApiErrorException_should_logAClientErrorAtInfo_when_theQueryJobIsUnknown() {
        // The other 4xx this service produces, and much the most common one.
        handle(new InvalidQueryJobException("20260903_120000_00001_abcde"));

        assertThat(theOnlyLoggedEvent().getLevel())
            .as("the level a 404 is reported at")
            .isEqualTo(Level.INFO);
    }

    @Test
    public void handleTableApiErrorException_should_logAServerErrorAtError() {
        handle(new IllegalStateException("something in here gave way"));

        assertThat(theOnlyLoggedEvent().getLevel())
            .as("the level a 500 is reported at")
            .isEqualTo(Level.ERROR);
    }

    @Test
    public void handleTableApiErrorException_should_logAStackTraceForAServerError() {
        handle(new IllegalStateException("something in here gave way"));

        assertThat(theOnlyLoggedEvent().getThrowableProxy())
            .as("the throwable attached to a 500")
            .isNotNull()
            .extracting(IThrowableProxy::getClassName)
            .isEqualTo(TableApiErrorException.class.getName());
    }

    @Test
    public void handleTableApiErrorException_should_answerTheStatusTheErrorCarries() {
        assertThat(handler()
            .handleTableApiErrorException(new TableApiErrorException(
                new MalformedClientSuppliedCredentialsException("not of the form name=value"),
                TableData::errorInstance))
            .getStatusCode()
            .value())
            .as("the status answered for a malformed credentials header")
            .isEqualTo(400);
    }
}
