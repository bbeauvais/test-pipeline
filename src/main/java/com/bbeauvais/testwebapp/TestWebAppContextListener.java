package com.bbeauvais.testwebapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbeauvais.testwebapp.servlet.CoucouServlet;
import com.bbeauvais.testwebapp.servlet.TestServlet;

@WebListener
public class TestWebAppContextListener implements ServletContextListener {

	// =========================================================================
	// ============================== CONSTANTES ===============================
	// =========================================================================
	private static final Logger LOGGER = LoggerFactory.getLogger(TestWebAppContextListener.class);

	// =========================================================================
	// ============================== METHODES PUBLIQUES =======================
	// =========================================================================
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOGGER.info("Initialisation des Servlets pour l'application web TestWebApp");
		
		sce.getServletContext().addServlet("Servlet", TestServlet.class).addMapping("/hello");
		sce.getServletContext().addServlet("Coucou servlet", CoucouServlet.class).addMapping("/coucou");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOGGER.info("Fermeture de l'application web TestWebApp {}", sce.getServletContext().getServerInfo());
	}

	// =========================================================================
	// ============================== METHODES PRIVEES =========================
	// =========================================================================

}
