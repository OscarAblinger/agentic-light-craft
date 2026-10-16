# Canonical reference textures (style anchors) live here.
#
# These are the visual "gold standard" for the LightCraft look. Agents compare
# new work against them and the style spec, not against their own taste.
#
# The files are deliberately unnamed (no machine names). They show the machine
# look: marble body and frame, a 1px outline.deep recessed edge around the
# front face, bright gold and lapis accents, emissive lux, no steel.
#
# - machine_front_01.png  marble front; a stacked pair of lapis gems above a
#                           lux firebox window (ramped from lux.specular), the
#                           window ringed by outline.deep, gold bolts.
# - machine_front_02.png  marble front; a radial lens - lux core, lapis rings,
#                           gold outer ring - with gold bolts at the corners.
# - machine_front_03.png  marble front; twin vertical gold rails around a
#                           square lapis core with a lux centre, a recessed
#                           outline.deep slot below.
#
# Check them with --category machines (they pass). Regenerate them with
# generators/MachineFronts.java; the files should come out byte-identical.
