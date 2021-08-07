This is an unofficial client-side mod for the Monumenta MMO server (www.playmonumenta.com).

## Feature List

### Firmament ping fix

Makes the "Firmament" item usable even at high ping (and may also be an improvement even with low ping).

### Trident texture fix

Shows custom trident textures of the resource pack when tridents are held instead of only when in the inventory.

**Important: The resource pack "Fabric Mods" needs to be above the Monumenta resource pack!** Otherwise, tridents will
be tiny when held.

The thrown trident projectiles still look like the vanilla trident, as changing that would require a server-side mod.

### Custom Helmet Models

Allows resource packs to define models for helmets. Compatible with OptiFine, in particular compatible with its CIT.

## Technical Documentation

### Creating a helmet model as a resource pack artist

1. Create your 3D model and put it anywhere in your resource pack. Make sure it has a proper `head` transformation
   defined.
2. Create a json file for the helmet you want the model to apply to, looking like this:
   ```json
   {
     "parent": "item/generated",
     "textures": {"layer0": "<icon texture path>"},
     "overrides": [{
        "predicate": {"on_head": 1},
        "model": "<absolute path to 3D model>"
      }]
   }
   ```
4. In the properties file of your helmet that has `type=item`, use `model=` instead of `texture=` and reference the json
   file created in step 2.

## Contribute

[The code is open source](https://github.com/Njol/UnofficialMonumentaMod) and pull requests for new features or fixes
are always welcome!