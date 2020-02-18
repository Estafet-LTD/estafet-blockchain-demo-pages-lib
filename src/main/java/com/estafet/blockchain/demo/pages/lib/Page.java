package com.estafet.blockchain.demo.pages.lib;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.PageFactory;

import com.estafet.openshift.boost.commons.lib.properties.PropertyUtils;

public abstract class Page {

	private final WebDriver driver;
	private final URL url;

	private final String regExSpecialChars = ":/<([{\\^-=$!|]})?*+.>";
	private final String regExSpecialCharsRE = regExSpecialChars.replaceAll(".", "\\\\$0");
	private final Pattern reCharsREP = Pattern.compile("[" + regExSpecialCharsRE + "]");

	public Page(WebDriver driver) {
		this.driver = driver;
		PageFactory.initElements(driver, this);
		try {
			this.url = new URL(driver.getCurrentUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public Page() {
		this("");
	}

	public Page(String... params) {
		try {
			this.url = new URL(getURI() + resolveUri(params));
			Capabilities capabilities = DesiredCapabilities.htmlUnitWithJs();
			driver = new HtmlUnitDriver(capabilities);
			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			PageFactory.initElements(driver, this);
			driver.get(url.toString());
			if (!isLoaded()) {
				throw new RuntimeException("cannot load url " + driver.getCurrentUrl());
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private String getURI() {
		return PropertyUtils.instance().getProperty("application.url");
	}

	public String title() {
		return "Estafet Blockchain Demo";
	}

	public boolean isLoaded(String... params) {
		String compare = getURI() + resolveUri(params);
		return compare.equals(driver.getCurrentUrl()) && driver.getTitle().equals(title());
	}

	public boolean isLoaded() {
		String currentUrl = driver.getCurrentUrl();
		if (uri().contains("{1}")) {
			String compare = escapeRegExChars(getURI())
					+ escapeRegExChars(uri().replaceAll("\\{(\\d+|0x[a-zA-Z0-9]+|[A-Z]{3})\\}", "REPLACE")).replaceAll("REPLACE", "\\(\\\\d+|0x[a-zA-Z0-9]+|[A-Z]{3})");
			return currentUrl.matches(compare) && driver.getTitle() != null && driver.getTitle().equals(title());
		} else {
			return url.toString().equals(currentUrl) && driver.getTitle().equals(title());
		}
	}

	private String escapeRegExChars(String s) {
		Matcher m = reCharsREP.matcher(s);
		return m.replaceAll("\\\\$0");
	}

	public void close() {
		driver.close();
	}

	protected void setField(WebElement element, String value) {
		element.clear();
		element.sendKeys(value);
	}

	protected void setField(WebElement element, Integer value) {
		element.clear();
		element.sendKeys(Integer.toString(value));
	}

	protected <T extends Page> T click(WebElement element, Class<T> clazz) {
		try {
			element.click();
			Constructor<T> constructor = clazz.getConstructor(WebDriver.class);
			return constructor.newInstance(driver);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected <T extends Page> T submit(WebElement element, Class<T> clazz) {
		try {
			element.sendKeys(Keys.ENTER);;
			Constructor<T> constructor = clazz.getConstructor(WebDriver.class);
			return constructor.newInstance(driver);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected <T extends Page> T click(String text, List<WebElement> items, Class<T> clazz) {
		for (WebElement item : items) {
			if (item.getText().equals(text)) {
				return click(item, clazz);
			}
		}
		throw new RuntimeException("cannot find link for " + text);
	}

	protected List<String> getTextList(List<WebElement> items) {
		List<String> names = new ArrayList<String>();
		for (WebElement item : items) {
			names.add(item.getText());
		}
		return names;
	}

	protected WebDriver getDriver() {
		return driver;
	}

	protected URL getUrl() {
		return url;
	}

	private String resolveUri(String... params) {
		String uri = uri();
		int index = 1;
		for (String param : params) {
			uri = uri.replaceAll("\\{" + index + "\\}", param);
			index++;
		}
		return uri;
	}

	public abstract String uri();

	public String getCurrentURI() {
		return driver.getCurrentUrl().substring(getURI().length());
	}

}
