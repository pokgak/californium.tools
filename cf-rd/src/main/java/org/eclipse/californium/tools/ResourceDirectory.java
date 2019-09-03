/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 ******************************************************************************/
package org.eclipse.californium.tools;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.tools.resources.RDLookUpTopResource;
import org.eclipse.californium.tools.resources.RDResource;
import org.eclipse.californium.tools.resources.RDTagTopResource;

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.tools.CredentialsUtil.Mode;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;

/**
 * The class ResourceDirectory provides an experimental RD
 * as described in draft-ietf-core-resource-directory-04.
 */
public class ResourceDirectory extends CoapServer {

	// exit codes for runtime errors
	public static final int ERR_INIT_FAILED = 1;

	public static final List<Mode> SUPPORTED_MODES = Arrays
	.asList(new Mode[] { Mode.PSK });

	// allows configuration via Californium.properties
	public static final int DTLS_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_SECURE_PORT);

	public static void main(String[] args) {

		System.out.println("Usage: java -jar ... [PSK] [ECDHE_PSK] [RPK] [X509] [NO_AUTH]");
		System.out.println("Default :            [PSK] [ECDHE_PSK] [RPK] [X509]");

		// create server
		CoapServer server = new ResourceDirectory();

		// explicitly bind to each address to avoid the wildcard address reply problem
		// (default interface address instead of original destination)
		for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
			if (!addr.isLinkLocalAddress()) {
				CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
				builder.setInetSocketAddress(new InetSocketAddress(addr, CoAP.DEFAULT_COAP_PORT));
				server.addEndpoint(builder.build());

				// add secure endpoint
				DtlsConnectorConfig.Builder dtlsBuilder = new DtlsConnectorConfig.Builder();
				CredentialsUtil.setupCid(args, dtlsBuilder);
				dtlsBuilder.setAddress(new InetSocketAddress(addr, DTLS_PORT));
				List<Mode> modes = CredentialsUtil.parse(args, CredentialsUtil.DEFAULT_SERVER_MODES, SUPPORTED_MODES);
				CredentialsUtil.setupCredentials(dtlsBuilder, CredentialsUtil.SERVER_NAME, modes);
				DTLSConnector connector = new DTLSConnector(dtlsBuilder.build());
				builder = new CoapEndpoint.Builder();
				builder.setConnector(connector);
				server.addEndpoint(builder.build());
			}
		}

		server.start();

		System.out.printf(ResourceDirectory.class.getSimpleName() + " listening on port %d.\n", server.getEndpoints().get(0).getAddress().getPort());
	}

	public ResourceDirectory() {

		RDResource rdResource = new RDResource();

		// add resources to the server
		add(rdResource);
		add(new RDLookUpTopResource(rdResource));
		add(new RDTagTopResource(rdResource));
	}
}
