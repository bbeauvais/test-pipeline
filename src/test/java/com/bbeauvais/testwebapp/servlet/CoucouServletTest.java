package com.bbeauvais.testwebapp.servlet;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CoucouServletTest {

	@InjectMocks
	private CoucouServlet testedObject;

	@Mock
	private HttpServletRequest requestMock;

	@Mock
	private HttpServletResponse responseMock;

	@Mock
	private PrintWriter writerMock;

	@Test
	public void testDoGet_whenNoException_thenResp200() throws Exception {
		when(responseMock.getWriter()).thenReturn(writerMock);

		testedObject.doGet(requestMock, responseMock);

		verify(responseMock, times(1)).setStatus(anyInt());

		verify(writerMock, times(1)).println("Coucou");
		verifyNoMoreInteractions(writerMock);
	}

	@Test
	public void testDoGet_whenIOException_thenResp500() throws Exception {
		when(responseMock.getWriter()).thenThrow(IOException.class);

		testedObject.doGet(requestMock, responseMock);

		verify(responseMock, times(1)).setStatus(anyInt());

		verify(writerMock, never()).println(anyString());
	}

}
