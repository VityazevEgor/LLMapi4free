package com.vityazev_egor.LLMs;

import com.vityazev_egor.NoDriver;
import com.vityazev_egor.Core.LambdaWaitTask;
import java.util.concurrent.atomic.AtomicReference;
import com.vityazev_egor.Core.WebElements.WebElement;

public class Shared {
    /**
     * Waits for the specified web elements to exist with a default timeout of 8 seconds.
     *
     * @param checkForClickable whether to check if the last element is clickable
     * @param elements the web elements to wait for
     * @return {@code true} if all elements exist (and the last one is clickable if required), {@code false} otherwise
     */
    public static Boolean waitForElements(Boolean checkForClickable, WebElement... elements){
        return waitForElements(checkForClickable, 8, elements);
    }

    /**
     * Waits for the specified web elements to exist within the given timeout.
     *
     * @param checkForClickable whether to check if the last element is clickable in addition to existence
     * @param timeOutSeconds the maximum time to wait in seconds
     * @param elements the web elements to wait for
     * @return {@code true} if all elements exist (and the last one is clickable if required), {@code false} otherwise
     */
    public static Boolean waitForElements(Boolean checkForClickable, Integer timeOutSeconds, WebElement... elements){
        return new LambdaWaitTask(() -> {
            for (WebElement element : elements) {
                if (!element.isExists()) return false;
            }
            return !checkForClickable || elements[elements.length - 1].isClickable();
        }).execute(timeOutSeconds, 400);
    }

    /**
     * Waits for the page content to stop changing, indicating that an answer has been fully generated.
     * This method monitors HTML changes and returns {@code true} when the content remains stable.
     *
     * @param driver the NoDriver instance to monitor
     * @param timeOutForAnswer the maximum time to wait for content stabilization in seconds
     * @param delayMilliseconds initial delay before starting to monitor changes
     * @return {@code true} if the content has stabilized (answer is complete), {@code false} if timeout occurred
     */
    public static Boolean waitForAnswer(NoDriver driver, Integer timeOutForAnswer, Integer delayMilliseconds){
        AtomicReference<String> previousHtml = new AtomicReference<>(driver.getHtml().orElse(""));
        
        com.vityazev_egor.Core.Shared.sleep(delayMilliseconds);
        
        return new LambdaWaitTask(() -> driver.getHtml().map(currentHtml -> {
            if (currentHtml.equals(previousHtml.get())) {
                return true;
            } else {
                previousHtml.set(currentHtml);
                return false;
            }
        }).orElse(false)).execute(timeOutForAnswer, delayMilliseconds);
    }

    public static class ProviderException extends Exception {
        public ProviderException(String message) {
            super(message);
        }    
    }


}
