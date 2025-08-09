---
name: Auto-Crafting Nested Recipes
description: Client-side mod functionality for auto-crafting nested recipes in Minecraft 1.20 Forge
alwaysApply: true
---

# Auto-Crafting Nested Recipes Implementation

## Overview
This rule defines the implementation approach for a client-side Minecraft Forge 1.20 mod feature that allows players to autocraft nested recipes in the crafting table.

## Requirements
1. Add a button to the Recipe Book GUI
2. Button should only appear if the player has all needed items (directly or through nested crafting)
3. Implement recursive recipe checking:
    - If a selected recipe needs intermediate ingredients not in player inventory
    - Check if those intermediate ingredients can be crafted from items the player has
    - Continue recursively until all ingredient requirements are satisfied or determined impossible

## Implementation Guidelines

### GUI Modifications
- Extend the Recipe Book GUI to add the auto-craft button
- Position the button near the existing crafting button
- Use appropriate Minecraft styling for consistency
- Add tooltip explaining the button's function

### Recipe Processing Logic
1. When a recipe is selected in the Recipe Book:
    - Check if player has all direct ingredients in inventory
    - If yes, show standard craft button
    - If no, recursively check if missing ingredients can be crafted
    - If all ingredients can be obtained through crafting, show the auto-craft button

2. Recursive checking algorithm:
    - For each missing ingredient, check if there's a recipe for it
    - For each potential recipe, check if player has all ingredients
    - If ingredients are missing, recursively check those ingredients
    - Implement cycle detection to prevent infinite recursion
    - Cache results to optimize performance

3. Auto-crafting execution:
    - When button is clicked, determine the crafting order (ingredients first)
    - Execute crafting operations in sequence
    - Handle inventory management and item movement
    - Provide visual feedback during the process

### Classes to Implement
1. `AutoCraftingButton` - UI element for the button
2. `RecipeChecker` - Logic to recursively check recipe availability
3. `AutoCraftingHandler` - Manages the crafting execution process
4. `RecipeTreeBuilder` - Builds the dependency tree of recipes

### Integration Points
- Hook into the Recipe Book GUI rendering
- Intercept recipe selection events
- Integrate with the crafting system for execution

## Technical Considerations
- Client-side only implementation
- Respect Minecraft's existing crafting mechanics
- Optimize for performance with caching
- Handle edge cases (circular recipes, etc.)
- Ensure compatibility with other mods
