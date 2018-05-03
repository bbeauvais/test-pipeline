package com.bbeauvais.testwebapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOGGER.info("Fermeture de l'application web TestWebApp");
	}

	// =========================================================================
	// ============================== METHODES PRIVEES =========================
	// =========================================================================

}
