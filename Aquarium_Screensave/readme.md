# HD Aquarium Screensaver

A beautiful, high-definition aquarium screensaver built in Java, inspired by the classic Windows 98 aquarium screensaver with modern enhancements.

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-8%2B-orange)
![License](https://img.shields.io/badge/license-MIT-green)

## Features

### 🐟 Intelligent Fish Behaviors
- **Schooling Fish**: Groups of fish that swim together using flocking algorithms
  - Cohesion: Move toward the center of the school
  - Alignment: Match velocity with nearby fish
  - Separation: Avoid crowding
- **Wandering Fish**: Solo fish that explore independently with random direction changes
- **Fast Swimmers**: Speedy fish that dart around with occasional bursts of speed

### 🎨 Visual Elements
- **HD Resolution**: Native 1920x1080 resolution with smooth 60 FPS animations
- **Light Rays**: 6 animated light beams streaming from the surface with dynamic swaying
- **Realistic Bubbles**: Rising bubbles with wobble physics and gradient effects
- **Decorations**:
  - 4 detailed rocks with natural textures
  - 3 branching coral formations in various colors
  - 1 glowing treasure chest with animated golden glow
- **Animated Seaweed**: Swaying aquatic plants that move with simulated water currents
- **Gradient Backgrounds**: Depth-enhanced water and sand floor

### ⚙️ Technical Features
- Delta-time based animations for consistent speed across different hardware
- Anti-aliasing for smooth, high-quality graphics
- Object-oriented design with inheritance and polymorphism
- Efficient rendering with double buffering
- Shadow effects and lighting for added depth

## Requirements

- **Java**: JDK 8 or higher
- **Display**: 1920x1080 or higher recommended
- **RAM**: 512MB minimum

## Installation

1. **Clone or download** the repository
2. **Compile** the Java file:
   ```bash
   javac AquariumScreensaver.java
   ```
3. **Run** the screensaver:
   ```bash
   java AquariumScreensaver
   ```

## Usage

### Running the Screensaver

Simply execute the compiled class:
```bash
java AquariumScreensaver
```

The aquarium will open in a window. To exit, close the window normally.

### Customization

You can easily customize various aspects of the screensaver by modifying the constants and parameters:

#### Fish Configuration
```java
// Adjust number of fish in schools
createFishSchool(300, 300, 6, FishBehavior.SCHOOLING);  // 6 fish in first school

// Add more solo fish
for (int i = 0; i < 3; i++) {
    fishList.add(new Fish(random.nextInt(WIDTH), random.nextInt(HEIGHT), FishBehavior.WANDERING));
}
```

#### Screen Resolution
```java
private static final int WIDTH = 1920;   // Change to your resolution
private static final int HEIGHT = 1080;
```

#### Frame Rate
```java
private static final int TARGET_FPS = 60;  // Adjust for performance
```

#### Bubble Spawn Rate
```java
Timer bubbleTimer = new Timer(300, e -> {  // Change 300 to adjust timing (ms)
    // Bubble spawning logic
});
```

#### Colors
Modify fish colors in the `Fish` constructor:
```java
Color[] schoolColors = {
    new Color(255, 140, 0),   // Orange
    new Color(255, 215, 0),   // Gold
    new Color(65, 105, 225),  // Royal Blue
    // Add more colors here
};
```

## Architecture

### Class Structure

```
AquariumScreensaver (JPanel)
├── Fish
│   ├── FishBehavior enum
│   │   ├── SCHOOLING
│   │   ├── WANDERING
│   │   └── FAST_SWIMMER
│   └── Schooling algorithm implementation
├── Bubble
├── Decoration (abstract)
│   ├── Rock
│   ├── Coral
│   └── TreasureChest
└── LightRay
```

### Key Algorithms

#### Schooling Behavior (Boids Algorithm)
The fish schooling is based on Craig Reynolds' Boids algorithm with three rules:
1. **Cohesion**: Steer towards the average position of nearby fish
2. **Alignment**: Match velocity with nearby fish
3. **Separation**: Avoid getting too close to other fish

```java
// Simplified schooling logic
cohesion = (averagePosition - fishPosition) * cohesionWeight
alignment = (averageVelocity - fishVelocity) * alignmentWeight
separation = sum(avoidanceVectors) / neighborCount

newVelocity = velocity + cohesion + alignment + separation
```

#### Bubble Physics
Bubbles rise with:
- Upward velocity with random variation
- Sine wave wobble for realistic movement
- Horizontal drift

## Performance Tips

1. **Lower FPS**: Reduce `TARGET_FPS` to 30 for slower computers
2. **Fewer Objects**: Reduce the number of fish, bubbles, or decorations
3. **Disable Effects**: Comment out shadow rendering or light rays for better performance
4. **Resolution**: Run at a lower resolution by changing `WIDTH` and `HEIGHT`

## Extending the Screensaver

### Adding New Fish Behaviors

1. Add a new behavior type to the enum:
```java
enum FishBehavior {
    SCHOOLING,
    WANDERING,
    FAST_SWIMMER,
    YOUR_NEW_BEHAVIOR  // Add here
}
```

2. Implement the behavior in the `Fish.update()` method:
```java
if (behavior == FishBehavior.YOUR_NEW_BEHAVIOR) {
    // Your custom behavior logic
}
```

### Adding New Decorations

1. Create a new class extending `Decoration`:
```java
class YourDecoration extends Decoration {
    YourDecoration(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    void draw(Graphics2D g2d) {
        // Your drawing code
    }
}
```

2. Add instances in `createDecorations()`:
```java
decorations.add(new YourDecoration(x, y));
```

### Adding New Effects

You can add:
- **Sharks** that chase fish
- **Jellyfish** with tentacles
- **Starfish** on rocks
- **Particle effects** for bubbles popping at surface
- **Day/night cycle** with color transitions
- **Fish feeding** animations
- **Sound effects** (requires Java Sound API)

## Troubleshooting

### Performance Issues
- Reduce number of fish and bubbles
- Lower the frame rate
- Disable anti-aliasing by removing the rendering hints
- Run at a lower resolution

### Display Issues
- Ensure your Java version supports Swing
- Check that your graphics drivers are up to date
- Try running in windowed mode instead of fullscreen

### Compilation Errors
- Verify you're using JDK 8 or higher
- Ensure all imports are available
- Check for typos in the code

## Future Enhancements

Planned features for future versions:
- [ ] Fullscreen mode with escape key exit
- [ ] Configuration file for easy customization
- [ ] More fish species with unique animations
- [ ] Predator fish that chase smaller fish
- [ ] Interactive mode (fish follow mouse)
- [ ] Sound effects and ambient music
- [ ] Day/night lighting cycle
- [ ] Seasons with different decorations
- [ ] Save/load custom aquarium setups
- [ ] Multi-monitor support

## Contributing

Contributions are welcome! Feel free to:
- Report bugs
- Suggest new features
- Submit pull requests
- Improve documentation

## Credits

Inspired by the classic Windows 98 aquarium screensaver.

Schooling algorithm based on Craig Reynolds' Boids (1986).

## License

MIT License - Feel free to use, modify, and distribute this code.

## Version History

### Version 1.0.0 (Current)
- Initial release
- Schooling fish behavior
- Multiple fish types (schooling, wandering, fast swimmers)
- Decorations (rocks, coral, treasure chest)
- Light rays and bubbles
- HD graphics with smooth animations

---

**Enjoy your relaxing virtual aquarium!** 🐠🌊✨
<BR>
![IMG_7739](https://github.com/user-attachments/assets/e06e4aca-3e84-43a9-a610-55b9ff2d95ad)
