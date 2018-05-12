package com.bbeauvais.testwebapp.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoucouServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CoucouServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Integer status = 200;
		
		try {
			resp.getWriter().println("Coucou");
		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage());
			status = 500;
		}
		
		resp.setStatus(status);
	}
	

}