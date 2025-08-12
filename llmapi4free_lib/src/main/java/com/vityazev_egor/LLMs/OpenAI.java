package com.vityazev_egor.LLMs;

import com.vityazev_egor.Core.CustomLogger;
import com.vityazev_egor.Core.LambdaWaitTask;
import com.vityazev_egor.Core.WebElements.By;
import com.vityazev_egor.Core.WebElements.WebElement;
import com.vityazev_egor.NoDriver;
import com.vityazev_egor.iChat;
import com.vityazev_egor.Models.ChatAnswer;

import java.awt.*;

public class OpenAI implements iChat{

    public static final String url = "https://chatgpt.com/";
    private final CustomLogger logger = new CustomLogger(OpenAI.class.getName());
    private final java.util.List<String> codeBlockBannerStyles = java.util.List.of(
                ".flex.items-center.text-token-text-secondary.px-4.py-2.text-xs.font-sans.justify-between.h-9.bg-token-sidebar-surface-primary.select-none.rounded-t-2xl",
                ".absolute.end-0.bottom-0.flex.h-9.items-center.pe-2"
            );

    private final NoDriver driver;
    public OpenAI(NoDriver driver) {
        this.driver = driver;
    }

    /**
     * Checks if the user is currently logged in.
     *
     * @return TRUE if the user is logged in, FALSE otherwise
     */
    private Boolean isLoggedIn() {
        var profileImage = driver.findElement(By.cssSelector("img[alt='Profile image']"));
        return Shared.waitForElements(false, 10, profileImage);
    }

    /**
     * Authenticates a user with Google account.
     * @return true if authentication is successful, false otherwise.
     */
    @Override
    public Boolean auth() {
        try {
            if (!createNewChat())
                throw new Exception("Can't create new chat");

            if (isLoggedIn())
                return true;

            var welcomeLoginButton = driver.findElement(By.cssSelector("button[data-testid='welcome-login-button']"));
            var loginButton = driver.findElement(By.cssSelector("button[data-testid='login-button']"));
            if (welcomeLoginButton.isExists())
                driver.getInput().emulateClick(welcomeLoginButton);
            else
                driver.getInput().emulateClick(loginButton);

            var googleAuthButton = driver.findElement(By.cssSelector("button[value='google']"));
            googleAuthButton.waitToAppear(10, 100);

            driver.getInput().emulateClick(googleAuthButton);
            return isLoggedIn();
        } catch (Exception ex) {
            logger.error("Authentication failed: " + ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Sends a prompt to the OpenAI chatbot and waits for an answer.
     *
     * @param prompt The user's input prompt to be sent to the AI.
     * @param timeOutForAnswer The maximum time (in seconds) to wait for an answer from the AI.
     * @return A {@link ChatAnswer} object containing the AI's response, HTML content, and a screenshot of the chat.
     */
    @Override
    public ChatAnswer ask(String prompt, Integer timeOutForAnswer) {
        try{
            // MAKE SURE THAT CHAT IS OPENED
            Boolean chatIsOpened = driver.getHtml().map(html->html.contains("ChatGPT")).orElse(false);
            if (!chatIsOpened){
                if (!auth())
                    throw new Exception("Can't open chat");
            }

            // SEND PROMPT
            boolean isPromptSent = false;
            var input = driver.findElement(By.id("prompt-textarea"));
            var stopResponseButton = driver.findElement(By.cssSelector("button[data-testid='stop-button']"));
            var sendButton = driver.findElement(By.cssSelector("button[data-testid='send-button']:not([disabled]"));
            for (int i=0; i<3; i++) {
                try {
                    input.waitToAppear(5, 100);
                    driver.getInput().insertText(input, prompt);
                    sendButton.waitToAppear(5, 100);
                    driver.getInput().emulateClick(sendButton);
                    stopResponseButton.waitToAppear(5, 100);
                    isPromptSent = true;
                    break;
                }
                catch (Exception ex){
                    logger.warning("Could not send prompt on iteration #" + i);
                    driver.getCurrentUrl().ifPresent(url -> driver.getNavigation().loadUrlAndWait(url, 10));
                }
            }
            if (!isPromptSent)
                throw new Exception("Can't send prompt");

            // WAIT FOR ANSWER AND GET TEXT, HTML AND IMAGE OF IT
            var waitForStopButtonToHide = new LambdaWaitTask(() -> !stopResponseButton.isExists());
            if (!waitForStopButtonToHide.execute(timeOutForAnswer, 100))
                throw new Exception("Can't get answer from AI in time");
            codeBlockBannerStyles.forEach(style->{
                int codeBlockBannersCount = driver.findElements(By.cssSelector(style)).size();
                logger.info(String.format("Found %d block to remove", codeBlockBannersCount));
                for (int i=0; i<codeBlockBannersCount; i++)
                    driver.findElement(By.cssSelector(style)).removeFromDOM();
            });
            var answerBlocks = driver.findElements(By.cssSelector("div[data-message-author-role='assistant']"));
            if (answerBlocks.isEmpty())
                throw new Exception("Could not get answer from OpenAI");
            var latestAnswer = answerBlocks.getLast();
            return new ChatAnswer(
                    latestAnswer.getText(),
                    latestAnswer.getHTMLContent(),
                    driver.getMisc().captureScreenshot()
            );
        }
        catch (Exception ex){
            logger.error("Error occurred while processing the prompt: " + ex.getMessage(), ex);
            return new ChatAnswer();
        }
    }

    /**
     * Creates a new chat by navigating to the OpenAI URL and attempting to bypass any CAPTCHA challenges.
     *
     * @return true if the chat was successfully created or no CAPTCHA needed, false otherwise
     */
    @Override
    public Boolean createNewChat() {
        driver.getNavigation().loadUrlAndWait(OpenAI.url, 10);
        var cfPleaseWait = driver.findElement(By.id("cf-please-wait"));

        if (!cfPleaseWait.isExists()) {
            logger.info("There is not need to bypass cf challenge");
            return true;
        }

        var bypassCf = new LambdaWaitTask(() -> {
            if (!cfPleaseWait.isExists()) return true;
            var spacer = driver.findElement(By.className("spacer"));
            try {
                Dimension size = spacer.getSize().orElseThrow(() -> new Exception("Could not get size of captcha"));
                Point position = spacer.getPosition().orElseThrow(() -> new Exception("Could not get position of captcha"));
                Double yClick = position.getY();
                Double xClick = position.getX() - size.getWidth() / 2 + 20;
                driver.getXdo().click(xClick, yClick);
                return false;
            } catch (Exception ex) {
                logger.error("Could not click on captcha", ex);
                return false;
            }
        });

        return bypassCf.execute(20, 500);
    }

    @Override
    public String getName() {
        return "OpenAI";
    }
    
}
