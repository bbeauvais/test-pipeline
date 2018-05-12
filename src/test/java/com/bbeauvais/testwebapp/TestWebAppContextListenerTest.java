package com.bbeauvais.testwebapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRegistration.Dynamic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestWebAppContextListenerTest {

	@Mock
	private ServletContextEvent contextEventMock;

	@Mock
	private ServletContext servletContextMock;

	@Mock
	private Dynamic dynamicMock;

	@InjectMocks
	private TestWebAppContextListener testedObject;

	@SuppressWarnings("unchecked")
	@Test
	public void testContextInitialized() throws Exception {
		when(contextEventMock.getServletContext()).thenReturn(servletContextMock);
		when(servletContextMock.addServlet(anyString(), any(Class.class))).thenReturn(dynamicMock);

		testedObject.contextInitialized(contextEventMock);

		verify(servletContextMock, times(2)).addServlet(anyString(), any(Class.class));
		verify(dynamicMock, times(2)).addMapping(any());
	}

	@Test
	public void testContextDestroyed() throws Exception {
		when(contextEventMock.getServletContext()).thenReturn(servletContextMock);

		testedObject.contextDestroyed(contextEventMock);

		verify(servletContextMock, times(1)).getServerInfo();
	}

}
