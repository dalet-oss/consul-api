package com.ecwid.consul;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilsTest {

	@Test
	public void testEncodeUrl() throws Exception {
		String uri = "http://example.com/path with spaces";
		String expected = "http://example.com/path%20with%20spaces";

		assertEquals(expected, Utils.encodeUrl(uri));
	}

	@Test
	public void testGenerateUrl_Simple() throws Exception {
		assertEquals("/some-url", Utils.generateUrl("/some-url", emptyList()));

		List<UrlParameters> urlParams = new ArrayList<>();
		urlParams.add(null);
		assertEquals("/some-url", Utils.generateUrl("/some-url", urlParams));

		urlParams.add(null);
		assertEquals("/some-url", Utils.generateUrl("/some-url", urlParams));
	}

	@Test
	public void testGenerateUrl_Parametrized() throws Exception {
		UrlParameters first = new SingleUrlParameters("key", "value");
		UrlParameters second = new SingleUrlParameters("key2");
		assertEquals("/some-url?key=value&key2", Utils.generateUrl("/some-url", List.of(first, second)));
	}

	@Test
	public void testGenerateUrl_Encoded() throws Exception {
		UrlParameters first = new SingleUrlParameters("key", "value value");
		UrlParameters second = new SingleUrlParameters("key2");
		UrlParameters third = new SingleUrlParameters("key3", "value!value");
		assertEquals("/some-url?key=value+value&key2&key3=value%21value", Utils.generateUrl("/some-url", List.of(first, second, third)));
	}

	@Test
	public void testToSecondsString() throws Exception {
		assertEquals("1000s", Utils.toSecondsString(1000L));
	}

	@Test
	public void testAssembleAgentAddressWithPath() {
		// Given
		String expectedHost = "http://host";
		int expectedPort = 8888;
		String expectedPath = "path";

		// When
		String actualAddress = Utils.assembleAgentAddress(expectedHost, expectedPort, expectedPath);

		// Then
		assertEquals(
			String.format("%s:%d/%s", expectedHost, expectedPort, expectedPath),
			actualAddress
		);
	}

	@Test
	public void testAssembleAgentAddressWithEmptyPath() {
		// Given
		String expectedHost = "http://host";
		int expectedPort = 8888;
		String expectedPath = "   ";

		// When
		String actualAddress = Utils.assembleAgentAddress(expectedHost, expectedPort, expectedPath);

		// Then
		assertEquals(
			String.format("%s:%d", expectedHost, expectedPort),
			actualAddress
		);
	}

	@Test
	public void testAssembleAgentAddressWithoutPath() {
		// Given
		String expectedHost = "https://host";
		int expectedPort = 8888;

		// When
		String actualAddress = Utils.assembleAgentAddress(expectedHost, expectedPort, null);

		// Then
		assertEquals(
			String.format("%s:%d", expectedHost, expectedPort),
			actualAddress
		);
	}
}
