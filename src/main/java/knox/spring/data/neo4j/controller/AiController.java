package knox.spring.data.neo4j.controller;

import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.*;

import knox.spring.data.neo4j.ai.GroupTools;
import knox.spring.data.neo4j.ai.DesignTools;
import knox.spring.data.neo4j.ai.OperatorTools;
import knox.spring.data.neo4j.domain.DesignSpace;

import knox.spring.data.neo4j.services.DesignSpaceService;

@RestController
public class AiController {

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;
    
    private final ChatModel chatModel;

    final DesignSpaceService designSpaceService;

    public AiController(ChatClient.Builder chatClientBuilder, ChatModel chatModel, DesignSpaceService designSpaceService) {
        this.chatModel = chatModel;
        this.designSpaceService = designSpaceService;
    }

    @PostMapping("/agent")
    public String agent(@RequestParam(value = "prompt", required = true) String prompt,
            @RequestBody(required = false) String system) {

        if (openAiApiKey.equals("ENTER_OPENAI_API_KEY_HERE")) {
            return "Error: Please set 'spring.ai.openai.api-key' in application.properties to use AI Chat Features.";
        }
        
        System.out.println("\n\nReceived prompt: " + prompt);

        ChatClient chatClient = ChatClient.create(chatModel);
        
        ChatResponse response = chatClient.prompt(prompt)
            .tools(new GroupTools(designSpaceService), 
                   new DesignTools(designSpaceService),
                   new OperatorTools(designSpaceService))
            .call().chatResponse();

        Usage usage = response.getMetadata().getUsage();
        int promptTokens = usage.getPromptTokens();
        int completionTokens = usage.getCompletionTokens();
        int totalTokens = usage.getTotalTokens();

        System.out.printf("\nTokens:\nPrompt: %d, Completion: %d, Total: %d%n", promptTokens, completionTokens, totalTokens);

        double costPerThousandPrompt = 0.0015; // rates for gpt-3.5-turbo
        double costPerThousandCompletion = 0.002;
        double cost = (promptTokens / 1000.0) * costPerThousandPrompt + (completionTokens / 1000.0) * costPerThousandCompletion;
        System.out.printf("Estimated cost: $%.4f%n", cost);

        System.out.println("\n\nAI response: " + response.getResult().getOutput().getText());
        return response.getResult().getOutput().getText().toString();
    }
    
}
