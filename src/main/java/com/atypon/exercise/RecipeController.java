package com.atypon.exercise;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/recipes")
public class RecipeController {
    // Init the logger object
    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);

    // Spoonacular recipe URLs
    public static final String SPOONACULAR_RECIPE_SEARCH_URL = "https://api.spoonacular.com/recipes/complexSearch";
    public static final String SPOONACULAR_RECIPE_INFO_URL = "https://api.spoonacular.com/recipes/$/information";
    public static final String SPOONACULAR_INGRADIENT_INFO_URL="https://api.spoonacular.com/food/ingredients/$/information";

    // Read API key of Spoonacular from API_KEY environment variable.
    @Value("${API_KEY:default-api-key}")
    private String apiKey;

    @Autowired
    private ObjectMapper objectMapper;


     // Define a DTO for the request body for /recipes/curry/information (customized recipe information)
     public static class RecipeRequest {
        private String[] ingredientsToExclude;

        public String[] getIngredientsToExclude() {
            return ingredientsToExclude;
        }

        public void setIngredientsToExclude(String[] ingredientsToExclude) {
            this.ingredientsToExclude = ingredientsToExclude;
        }
    }


    // GET /recipes/search API with query parameters
    @GetMapping("/search")
    public String searchRecipes(@RequestParam String name, @RequestParam int number) {

        logger.info("GET /recipes/search?name={}&number={}",name, number);

        //logger.info("API_KEY used: {}" , apiKey);
        // Create RestTemplate instance to make HTTP requests
        RestTemplate restTemplate = new RestTemplate();

        // Build the URL with query parameters
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(SPOONACULAR_RECIPE_SEARCH_URL)
                .queryParam("query", name)
                .queryParam("number", number)
                .queryParam("addRecipeInformation", false);

        // Create HTTP headers and set the API key
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);

        // Create the HTTP entity containing headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    builder.toUriString(), // Full URI with query parameters
                    HttpMethod.GET,        // HTTP GET method
                    entity,                // HTTP entity containing headers
                    String.class           // Expected response type
            );

            // Return the response body as a string
            return response.getBody();
        } catch(Exception e){
           // e.printStackTrace();
            return "An error occured: " + e.getMessage();
        }
    }

    // GET /recipes/search API with query parameters
    @GetMapping("/{name}/information")
    public ResponseEntity<String> getRecipeInfo(@PathVariable String name) {
            logger.info("GET /recipes/{}/information}",name);
            try{
            Map<String, Pair<BigDecimal, String>> ingradients = GetIngredients(name);
            BigDecimal [] allCalories = fetchAllCalories(ingradients, null);
            BigDecimal allCaloriesSum = sumAllCalories(allCalories);
            // Return total calories as a string
            return ResponseEntity.status(HttpStatus.OK).body(allCaloriesSum.toString()); 
        } catch(Exception e){
            // Log the error and return 500 Internal Server Error
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage()); 
        }
    }
    // fetchIngradientIds a helper function to get the list of ingradient IDs from the request body which represents the recipe information.
    public Map<String, Pair<BigDecimal, String>> fetchIngradientIds(String body){
        Map<String, Pair<BigDecimal, String>> ingredientDetailsMap = new HashMap<>();
        try{
        JsonNode jsonNode = objectMapper.readTree(body);
        JsonNode extendedIngredientsNode = jsonNode.get("extendedIngredients");
        if(extendedIngredientsNode !=null && extendedIngredientsNode.isArray()){
            // Iterate over the array of ingredients
            Iterator<JsonNode> ingredientsIterator = extendedIngredientsNode.elements();
            
            for(int i=0 ; i<extendedIngredientsNode.size();i++) {
                JsonNode ingredient = ingredientsIterator.next();
                // Fetch "id" field from each ingradient
                String id = ingredient.get("id").asText();
                
                
                JsonNode measuresNode = ingredient.get("measures").get("metric");
                BigDecimal amount = new BigDecimal(measuresNode.get("amount").asText());
                String unitShort = measuresNode.get("unitShort").asText();

                ingredientDetailsMap.put(id, Pair.of(amount, unitShort));
            }
        }

        return ingredientDetailsMap;

    }catch(Exception e){
        logger.error("error while fetching ingradient ids from request body {}", e);
        return null;
    }
    }
    // fetchAllCalories this function takes a map of ingredient id as the key, and a pair of amount and unit for that ingredient, and then all calories as an array
    public BigDecimal[] fetchAllCalories(Map<String, Pair<BigDecimal, String>> ingredientIds, Map<String, Boolean> execludedIngradients) {
        BigDecimal[] allCalories = new BigDecimal[ingredientIds.size()];  // Array to store all calorie values
        RestTemplate restTemplate = new RestTemplate();
        logger.info("ingradient ids: {} execuleded: {}", ingredientIds, execludedIngradients);
        int i = 0;
        for (Map.Entry<String, Pair<BigDecimal, String>> entry : ingredientIds.entrySet()) {
            // Access the key (ingredient ID)
            String id = entry.getKey();

            // Access the value (amount and unit) stored in the Pair
            BigDecimal amount = entry.getValue().getLeft(); 
            String unitShort = entry.getValue().getRight(); 

            try {
                // Construct the URL for each ingredient's information
                String ingredientUrl = SPOONACULAR_INGRADIENT_INFO_URL.replace("$", id);
    
                // Create the request entity with headers
                HttpHeaders headers = new HttpHeaders();
                headers.set("x-api-key", apiKey);
                HttpEntity<String> entity = new HttpEntity<>(headers);
    
                // Build the complete URL with the amount query parameter
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(ingredientUrl)
                        .queryParam("amount", amount)
                        .queryParam("unit",unitShort);
                         
    
                // Make the GET request to fetch the ingredient's information
                ResponseEntity<String> response = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        entity,
                        String.class
                );
    
                // Parse the response body to JSON
                String body = response.getBody();
                JsonNode jsonNode = objectMapper.readTree(body);
    
                // Extract the nutrition section and find the "Calories" field
                JsonNode nutrientsNode = jsonNode.get("nutrition").get("nutrients");

                // Extract name of the ingredient
                String ingredientName = jsonNode.get("original").asText();

                // Iterate over the nutrients to find the "Calories" value
                for (JsonNode nutrient : nutrientsNode) {
                    if (nutrient.get("name").asText().equals("Calories") )  {
                        if  ( execludedIngradients !=null && execludedIngradients.containsKey(ingredientName)){
                            break;
                        }
                        BigDecimal calories = new BigDecimal(nutrient.get("amount").asText());
                        allCalories[i] = calories;  // Store the calories in the array
                        logger.info("Calories for {} {} of {}: {}", amount, unitShort, ingredientName, calories);
                        break;
                    }
                }
                i++;
            } catch (Exception e) {
                logger.error("Error fetching calories for ingredient {}: {}", id, e.getMessage(), e);
                allCalories[i] = BigDecimal.ZERO;  // Set 0 calories if there was an error
            }
        }
        return allCalories;
    }
    
    public BigDecimal sumAllCalories(BigDecimal[] allCalories) {
        BigDecimal totalCalories = BigDecimal.ZERO;  // Initialize the total calories as zero
    
        // Sum up the calories
        for (BigDecimal calories : allCalories) {
            if (calories !=null) {
                totalCalories = totalCalories.add(calories);
            }
        }
    
        logger.info("Total Calories: {}", totalCalories);
        return totalCalories;
    }

public Map<String, Pair<BigDecimal, String>> GetIngredients(String name) {

    RestTemplate restTemplate = new RestTemplate();
    // Build the URL with query parameters
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(SPOONACULAR_RECIPE_SEARCH_URL)
    .queryParam("query", name)
    .queryParam("number", 1);
        
    // Create HTTP headers and set the API key
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-api-key", apiKey);

    // Create the HTTP entity containing headers
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(), 
                HttpMethod.GET,       
                entity,
                String.class
        );

        String body = response.getBody();
        JsonNode jsonNode = objectMapper.readTree(body);
        // Extract the "id" field from the first array JSON element of the response
        JsonNode firstResult = jsonNode.get("results").get(0);
        String recipId = firstResult.get("id").asText();
        logger.info("recipe ID fetched: {} for {}", recipId, name);

        // Make GET request to fetch recipe ingradients
        String url = SPOONACULAR_RECIPE_INFO_URL.replace("$", recipId);
        logger.info("URL used: {}", url);
        builder = UriComponentsBuilder.fromHttpUrl(url)
            .queryParam("includeNutrition", false);
            
            ResponseEntity<String> recipeInfoResponse = restTemplate.exchange(
                builder.toUriString(), 
                HttpMethod.GET,       
                entity,
                String.class
        );
    
        String recipeInfoBody = recipeInfoResponse.getBody();

        Map<String, Pair<BigDecimal, String>>  ingradients = fetchIngradientIds(recipeInfoBody);

        return ingradients;

        }catch(Exception e){
            logger.error("getting ingredients for recipe: {} {}", name, e.getMessage());
            return null;
        }
}


     // POST /recipes/curry/information API
     @PostMapping("/{name}/information")
     public ResponseEntity<String> getCustomizedRecipeInfo(@RequestBody RecipeRequest request, @PathVariable String name) {
        logger.info("POST /recipes/{}/information", name);
        try{
        Map<String, Pair<BigDecimal, String>> ingradients = GetIngredients(name);
         // Access the ingredients to exclude from the request body
         if (ingradients == null) {
            logger.error("error getting ingradients");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error getting ingradients"); 
         }
         Map<String, Boolean> ingredientsToExcludeMap  = new HashMap<>();
         String[] ingredientsToExcludeArray = request.getIngredientsToExclude();
         for (int i = 0;i< ingredientsToExcludeArray.length;i++){
            ingredientsToExcludeMap.put(ingredientsToExcludeArray[i], null);
         }
         BigDecimal [] allCalories = fetchAllCalories(ingradients, ingredientsToExcludeMap);
         BigDecimal totalSum = sumAllCalories(allCalories);
         return ResponseEntity.status(HttpStatus.OK).body(totalSum.toString()); 
        }catch (Exception e){
            logger.error("error getting customized recipe infromation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); 
        }
     }
}
