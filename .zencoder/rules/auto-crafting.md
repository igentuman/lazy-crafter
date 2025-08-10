# JEI Auto-Crafting Integration

## Overview
This rule defines the implementation approach for integrating the auto-crafting feature with JEI (Just Enough Items) in our Minecraft Forge 1.20 mod. This integration will allow players to use the auto-crafting functionality directly from JEI's recipe display.

## Requirements
1. Add an auto-craft button to JEI's crafting recipe display
2. Button should only appear when:
    - The player doesn't have all direct ingredients for a recipe
    - The player has ingredients to craft the missing ingredients (nested crafting)
3. Maintain consistent behavior with the vanilla Recipe Book implementation
4. Ensure compatibility with different JEI versions

## Implementation Guidelines

### JEI Plugin
- Create a JEI plugin class to register custom handlers
- Implement the `IModPlugin` interface
- Register GUI handlers and recipe transfer handlers

### GUI Integration
- Add the auto-craft button near JEI's existing "+" button
- Use the same visual style as the existing auto-craft button
- Show tooltips explaining the button's function
- Handle mouse interactions properly

### Recipe Transfer Logic
- Create a custom recipe transfer handler
- Check if recipes can be auto-crafted before transferring
- Integrate with the existing `AutoCraftingHandler`
- Maintain compatibility with JEI's standard transfer behavior

### Classes to Implement
1. `LazyCrafterJEIPlugin` - Main JEI integration point
2. `JEIAutoCraftingOverlay` - Handles button rendering and interaction
3. `AutoCraftingTransferHandler` - Custom recipe transfer handler

### Integration Points
- Hook into JEI's recipe display rendering
- Intercept recipe transfer events
- Reuse existing auto-crafting logic

## Technical Considerations
- Client-side only implementation
- Soft dependency on JEI (mod should work without JEI)
- Graceful error handling if JEI is not present
- Maintain consistent user experience between Recipe Book and JEI

## Code Structure
```java
// Main JEI plugin class
@JeiPlugin
public class LazyCrafterJEIPlugin implements IModPlugin {
    // Register GUI handlers and recipe transfer handlers
}

// GUI overlay handler
public class JEIAutoCraftingOverlay implements IGuiContainerHandler<AbstractContainerScreen<?>> {
    // Add auto-craft button to JEI's recipe display
}

// Recipe transfer handler
public class AutoCraftingTransferHandler implements IRecipeTransferHandler<AbstractContainerMenu, CraftingRecipe> {
    // Handle recipe transfers with auto-crafting support
}
