export enum FoodAllergyType {
  GLUTEN = 'gluten', // Wheat, barley, rye (celiac disease)
  DAIRY = 'dairy', // Milk, cheese, yogurt (lactose intolerance)
  EGGS = 'eggs',
  FISH = 'fish',
  SHELLFISH = 'shellfish', // Shrimp, crab, lobster
  TREE_NUTS = 'tree_nuts', // Almonds, cashews, walnuts
  PEANUTS = 'peanuts', // Often severe
  SOY = 'soy',
  SESAME = 'sesame',
  PORK = 'pork', // Common in Vietnam
  BEEF = 'beef',
  SPICY = 'spicy', // Not really an allergy but common intolerance
  NONE = 'none', // No allergies
}
