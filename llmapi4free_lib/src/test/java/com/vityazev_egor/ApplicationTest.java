package com.vityazev_egor;

import org.junit.jupiter.api.Test;

import com.vityazev_egor.Wrapper.LLMproviders;
import com.vityazev_egor.Wrapper.WrapperMode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class ApplicationTest {

    @Test
    void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    void duckduckChatIsNotOpened() throws IOException{
        var wrapper = new Wrapper("127.0.0.1:2080",LLMproviders.DuckDuck, WrapperMode.ExamMode);
        var answer = wrapper.askLLM("How are you today?",40);
        assertTrue(answer.getCleanAnswer().isPresent());
        answer = wrapper.askLLM("Write hello world in java", 40);
        assertTrue(answer.getCleanAnswer().isPresent());
        wrapper.exit();
    }

    @Test
    void copilotAuth() throws IOException{
        var wrapper = new Wrapper("127.0.0.1:2080",LLMproviders.Copilot, WrapperMode.ExamMode);
        Path pwdPath = Paths.get(System.getProperty("user.home"), "Desktop", "bingp.txt");
        List<String> data = Files.readAllLines(pwdPath);
        String loging = data.get(0);
        String password = data.get(1);
        var result = wrapper.auth(LLMproviders.Copilot, loging, password);
        assertTrue(result);
        wrapper.exit();
    }

    @Test
    void copilotAnswer() throws IOException{
        var wrapper = new Wrapper("127.0.0.1:2080",LLMproviders.Copilot, WrapperMode.ExamMode);
        Path pwdPath = Paths.get(System.getProperty("user.home"), "Desktop", "bingp.txt");
        List<String> data = Files.readAllLines(pwdPath);
        String loging = data.get(0);
        String password = data.get(1);
        var result = wrapper.auth(LLMproviders.Copilot, loging, password);
        assertTrue(result);
        var answer = wrapper.askLLM("Can you show me how to use for loop in Go?",60);

        var firstAnswer = answer.getCleanAnswer();
        assertTrue(firstAnswer.isPresent());
        System.out.println(firstAnswer);
        answer = wrapper.askLLM("Can you write hello world in java?",60);
        assertTrue(answer.getCleanAnswer().isPresent() && !answer.getCleanAnswer().get().equalsIgnoreCase(firstAnswer.get()));
        answer = wrapper.askLLM("Can you show me how to use for loop in java?",60);
        assertTrue(answer.getCleanAnswer().isPresent() && !answer.getCleanAnswer().get().equalsIgnoreCase(firstAnswer.get()));
        System.out.println(answer.getCleanAnswer());
        wrapper.exit();
    }

    @Test
    void testRotatingSystem() throws IOException{
        var wrapper = new Wrapper("127.0.0.1:2080",LLMproviders.Copilot, WrapperMode.ExamMode);
        assertTrue(wrapper.getWorkingLLM().isPresent() && wrapper.getWorkingLLM().get().getProvider() == LLMproviders.DuckDuck);

        Path pwdPath = Paths.get(System.getProperty("user.home"), "Desktop", "bingp.txt");
        List<String> data = Files.readAllLines(pwdPath);
        String loging = data.get(0);
        String password = data.get(1);

        var result = wrapper.auth(LLMproviders.Copilot, loging, password);
        assertTrue(result);
        assertTrue(wrapper.getWorkingLLM().isPresent() && wrapper.getWorkingLLM().get().getProvider() == LLMproviders.Copilot);
        wrapper.exit();
    }
}
