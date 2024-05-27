package com.bingchat4urapp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.awt.image.BufferedImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class BingChat {
    public EdgeBrowser _browser;
    private final Duration timeOutTime = java.time.Duration.ofSeconds(10);

    private final Logger logger = LogManager.getLogger(com.bingchat4urapp.BingChat.class);

    public BingChat(String proxy, int width, int height, int DebugPort){
        _browser = new EdgeBrowser(proxy, width, height, DebugPort);
    }


    // I need to fix it cuz there is sometime different types of auth
    public Boolean Auth(String login, String password){
        if (!_browser.LoadAndWaitForComplete("https://bing.com", timeOutTime, 0)) return false;
        _browser.CleanCookies();
        logger.info("I deleted all cocokies for the bing.com. Going to load site again");
        _browser._driver.get("https://google.com"); // got damn that thing is not good
        
        if (!_browser.LoadAndWaitForComplete("https://bing.com", timeOutTime, 0)) return false;
        logger.info("Loaded bing");

        if (!_browser.WaitForElement(timeOutTime, By.id("id_s"))) return false;
        //_browser._driver.findElement(By.id("id_s")).click();
        new Actions(_browser._driver).moveToElement(_browser._driver.findElement(By.id("id_s"))).click().perform();
        logger.info("Clicked on login button");

        if (_browser.WaitForElement(timeOutTime, By.cssSelector(".id_accountItem"))){
            logger.info("Detected second type of auth");
            _browser._driver.findElement(By.cssSelector(".id_accountItem")).click();
        }
        
        if (!_browser.WaitForComplete(timeOutTime, 4000)) return false;
        logger.info("Loaded login page");
        if (!_browser.WaitForElement(timeOutTime, By.name("loginfmt"))) return false;
        _browser._driver.findElement(By.name("loginfmt")).sendKeys(login);
        _browser._driver.findElement(By.id("idSIButton9")).click();
        logger.info("Entered login and clicked next button");

        if (!_browser.WaitForElement(timeOutTime, By.name("passwd"))) return false;
        _browser._driver.findElement(By.name("passwd")).sendKeys(password);
        _browser._driver.findElement(By.id("idSIButton9")).click();
        logger.info("Entered password");

        if (!_browser.WaitForComplete(timeOutTime, 0) || !_browser.WaitForElement(timeOutTime, By.id("acceptButton"))) return false;
        logger.info("Loadded 'Stay signed' page");
        _browser._driver.findElement(By.id("acceptButton")).click();

        if (!_browser.WaitForComplete(timeOutTime, 0) || !_browser.WaitForElement(timeOutTime, By.id("bnp_btn_accept"))) return false;
        _browser._driver.findElement(By.id("bnp_btn_accept")).click();
        logger.info("Finished auth!");

        // _browser.GetHtml("bing.html");
        // _browser.TakeScreenshot("logintest.png");

        return true;
    }

    // method that opens chat with bing and select specific conversation mode
    public Boolean CreateNewChat(int ModeType){
        if (!_browser.LoadAndWaitForComplete("https://www.bing.com/search?q=Bing+AI&showconv=1&FORM=hpcodx", java.time.Duration.ofSeconds(5),0)) return false;
        logger.info("Loaded chat");

        if (!_browser.WaitForElement(timeOutTime, By.cssSelector(".cib-serp-main"))){
            logger.error("Can't find main");
            return false;
        }
        logger.info("Found main block");

        SearchContext main = _browser._driver.findElement(By.cssSelector(".cib-serp-main")).getShadowRoot();
        if (!_browser.WaitForElement(timeOutTime, By.cssSelector("#cib-conversation-main"), main)){
            logger.error("Can't find conversation main");
            return false;
        }
        logger.info("Found conversation block");

        SearchContext ConversationMain = main.findElement(By.cssSelector("#cib-conversation-main")).getShadowRoot();
        if (!_browser.WaitForElement(timeOutTime, By.cssSelector("cib-welcome-container"), ConversationMain)){
            logger.error("Can't find welcome container");
            return false;
        }
        logger.info("Found welcome container");

        SearchContext WelcomeContainer = ConversationMain.findElement(By.cssSelector("cib-welcome-container")).getShadowRoot();
        if (!_browser.WaitForElement(timeOutTime, By.cssSelector("cib-tone-selector"), WelcomeContainer)){
            logger.error("Can't find tone selector");
            return false;
        }
        logger.info("Found tone selector block");

        SearchContext ToneSelector = WelcomeContainer.findElement(By.cssSelector("cib-tone-selector")).getShadowRoot();
        if (!_browser.WaitForElement(timeOutTime, By.cssSelector(".tone-precise"), ToneSelector)){
            logger.error("Can't find tone-precise option");
            return false;
        }
        logger.info("Found options for conversation type");
        
        WebElement MorePrecise = ToneSelector.findElement(By.cssSelector(".tone-precise"));
        WebElement Balanced = ToneSelector.findElement(By.cssSelector(".tone-balanced"));
        WebElement Creative = ToneSelector.findElement(By.cssSelector(".tone-creative"));

        Instant start = Instant.now();
        Boolean ElemtsOnTheCorrectPositions = false;
        
        // scroll web page down to botoom
        new Actions(_browser._driver).scrollByAmount(0, 1000);

        while (Duration.between(Instant.now(), start).getSeconds() <= timeOutTime.getSeconds()) {
            if (CheckElemntPosition(Creative) && CheckElemntPosition(Balanced) && CheckElemntPosition(MorePrecise)){
                ElemtsOnTheCorrectPositions = true;
                break;
            }
            try{
                Thread.sleep(500);
            }
            catch (InterruptedException e){}
        }

        if (!ElemtsOnTheCorrectPositions){
            logger.error("Could not load elements to select chat mode");
            return false;
        }

        switch (ModeType) {
            case 1:
                new Actions(_browser._driver).moveToElement(Creative).click().build().perform();  
                break;
            
            case 2:
                new Actions(_browser._driver).moveToElement(Balanced).click().build().perform();
                break;
                    
            default:
                new Actions(_browser._driver).moveToElement(MorePrecise).click().build().perform();
                break;
        }

        logger.info("Clicked on option");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("For some reason i can not stop thread", e);
        }
        //_browser.TakeScreenshot("SelectedMode.png");
        return true;
    }

    // method that checks if element position can be accesed by new Actions
    private Boolean CheckElemntPosition(WebElement Element){
        Point pos = Element.getLocation();
        java.awt.Dimension BrowserSize = _browser.GetBrowserSize();

        if (pos.getX()>=0 && pos.getX()<=BrowserSize.getWidth() && pos.getX()>=0 && pos.getX()<=BrowserSize.getHeight()){
            return true;
        }
        else{
            logger.warn("Elemet out of bounds");
            return false;
        }
    }

    // TimeOutForAnswer in seconds. This method must be called only after CreateNewChat
    public String AskBing(String promt, long TimeOutForAnswer){

        SearchContext actionBarContext = _browser._driver.findElement(By.cssSelector(".cib-serp-main")).getShadowRoot()
            .findElement(By.cssSelector("#cib-action-bar-main")).getShadowRoot();

        if (!_browser.WaitForElement(timeOutTime, By.cssSelector("cib-text-input"), actionBarContext)){
            logger.error("Can't find action bar");
            return null;
        }

        WebElement textInput = actionBarContext.findElement(By.cssSelector("cib-text-input")).getShadowRoot().findElement(By.cssSelector("#searchbox"));
        promt = promt.replace("\n", "").replace("\r", "");

        new Actions(_browser._driver).moveToElement(textInput).click().sendKeys(promt+"\n").perform();
        logger.info("I sent promt");

        // time when we started waiting for answer from bing
        Instant TimeStart = Instant.now();

        // Experiment code
        WebElement StopTypingButton = _browser._driver.findElement(By.cssSelector(".cib-serp-main")).getShadowRoot()
            .findElement(By.cssSelector("#cib-action-bar-main")).getShadowRoot()
            .findElement(By.cssSelector("cib-typing-indicator")).getShadowRoot()
            .findElement(By.cssSelector("#stop-responding-button"));

        while (!"true".equalsIgnoreCase(StopTypingButton.getAttribute("disabled"))) {
            if (Duration.between(TimeStart, Instant.now()).toSeconds()>=TimeOutForAnswer){
                logger.error("Could not get answer in time");
                _browser.TakeScreenshot("cantGetAnswer.png");
                return null;
            }

            try{
                Thread.sleep(500);
            }
            catch (Exception e){
                logger.error("Can't stop thread for some reason", e);
            }
        }

        return ExtractBingAnswers(_browser.GetHtml()).replace("Received message.", "");
    }

    // method that change zoom, takescreen, reset zoom ans scroold page to the end
    public BufferedImage TakeScreenOfAsnwer(String path){
        setZoom(70);
        BufferedImage result = null;
        if (path != null){
            result = _browser.TakeScreenshot(path);
        }
        else{
            result = _browser.TakeScreenshot();
        }
        setZoom(100);
        new Actions(_browser._driver).keyDown(Keys.CONTROL).sendKeys(Keys.END).keyUp(Keys.CONTROL).perform();
        return result;
    }

    private void setZoom(Integer percentage){
        JavascriptExecutor jsexec =  (JavascriptExecutor)_browser._driver;
        jsexec.executeScript("document.body.style.zoom = '"+percentage+"%'");
    }

    // method that gets raw text of bing answer
    private String ExtractBingAnswers(String html) {
        List<String> results = new ArrayList<>();
        if (html == null) return "";

        Document doc = Jsoup.parse(html);
        Elements contentNodes = doc.select("#CIBLiveRegion");
        for (Element node : contentNodes) {
            results.add(node.html().replace("<br>", "\n"));
        }

        if (results.size()>0){
            return results.get(results.size()-1);
        }
        else{
            return "";
        }
    }


    public void Exit(){
        _browser.Exit();
    } 
}
