package com.bingchat4urapp_server.bingchat4urapp_server.Controlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.bingchat4urapp_server.bingchat4urapp_server.BgTasks.CommandsExecutor;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskRepo;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.PromptCacheRepo;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskModel;
import com.bingchat4urapp_server.bingchat4urapp_server.Models.TaskType;
import com.vityazev_egor.Wrapper.LLMproviders;

@Controller
public class GUIController {

    @Autowired
    private TaskRepo context;

    @Autowired
    private PromptCacheRepo promptCacheRepo;

    @Autowired
    private Utils utils;

    @Autowired
    private CommandsExecutor executor;

    private final Logger logger = LoggerFactory.getLogger(GUIController.class);

    @GetMapping("/")
    public ModelAndView main(){
        var model = new ModelAndView("main");

        var promptModels = context.findLatestFinishedPromptTasks();
        for (TaskModel currentPrompt : promptModels) {
            if (currentPrompt.result != null && currentPrompt.result.length() >60){
                currentPrompt.result = currentPrompt.result.substring(0,60);
            }
        }
        model.addObject("latestPrompts", promptModels);
        String aiInUse = executor.getWrapper().getWorkingLLM().map(llm -> llm.getChat().getName()).orElse("None");
        model.addObject("aiInUse", aiInUse);

        model.addObject("promptCache", promptCacheRepo.count() == 0 ? "" : promptCacheRepo.findAll().get(0).getPrompt());
        return model;
    }

    @GetMapping("/ask")
    public ModelAndView ask(@RequestParam(name = "p") String prompt) {
        var newTask = utils.createPromptTask(prompt, "120");
        context.save(newTask);
        return new ModelAndView("redirect:/task/" + newTask.id);
    }

    @GetMapping("/auth")
    public ModelAndView auth() {
        return new ModelAndView("auth", "llmList", executor.getWrapper().getLlms());
    }

    @PostMapping("/auth")
    public String createAuthTask(@RequestParam String provider) {
        var newTask = utils.createAuthTask(provider);
        context.save(newTask);
        return "redirect:/task/" + newTask.id;
    }

    @GetMapping("/newchat")
    public String createNewChat(){
        var newTask = utils.createNewChatTask();
        context.save(newTask); 
        return "redirect:/task/" + newTask.id;
    }

    @PostMapping("/send")
    public String sendPrompt(@RequestParam String prompt){
        var newTask = utils.createPromptTask(prompt, "120");
        context.save(newTask);
        return "redirect:/task/" + newTask.id;
    }
    

    @RequestMapping(value = "/task/{taskid}", method = RequestMethod.GET)
    public ModelAndView waitTask(@PathVariable("taskid") Integer taskId) {
        var taskModel = context.findById(taskId).orElse(null);

        // если авторизация или создание чата прошло успешна, то сразу редериктим на страницу для отправки запросов
        if ((taskModel.taskType == TaskType.AUTH || taskModel.taskType == TaskType.CREATE_CHAT) && !taskModel.gotError && taskModel.isFinished){
            return new ModelAndView("redirect:/");
        }

        var view = new ModelAndView("taskwait");
        view.addObject("currentTask", taskModel);

        // Добовляем массив строк для того, чтобы построчно вывести ответ
        if (!taskModel.gotError && taskModel.isFinished && taskModel.result != null){
            view.addObject("answerLines", taskModel.result.split("\n"));
        }
        return view;
    }

    @GetMapping("providers")
    public ModelAndView getProviders() {
        var model = new ModelAndView("providers", "providers", executor.getWrapper().getLlms());
        String aiInUse = executor.getWrapper().getWorkingLLM().map(llm -> llm.getChat().getName()).orElse("None");
        model.addObject("aiInUse", aiInUse);
        return model;
    }

    @GetMapping("providers/resetStates")
    public ModelAndView resetProvidersStates() {
        executor.getWrapper().resetErrorStates();
        return new ModelAndView("redirect:/providers");
    }

    @RequestMapping(value = "providers/setPrefered/{providerName}", method=RequestMethod.GET)
    public ModelAndView requestMethodName(@PathVariable("providerName") LLMproviders provider) {
        executor.getWrapper().setPreferredProvider(provider);
        logger.info("Updated provider: " + provider);
        return new ModelAndView("redirect:/providers");
    }    
}
