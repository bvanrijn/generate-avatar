// Run this script in the console on http://www.colors.commutercreative.com/grid/
// Copy the output to a file named HTMLColor.java

var htmlColors = [...document.querySelector("#portfolio").children]
  .map(li => {
    const namedColor = li.style.backgroundColor;
    const rgb = getComputedStyle(li).backgroundColor;

    return [namedColor, rgb.replace("rgb", "new Color")];
  })
  .sort(([namedColorA, _ar], [namedColorB, _br]) => namedColorA.localeCompare(namedColorB))
  .map(
    ([namedColor, colorDecl]) =>
      `static final Color ${namedColor.toUpperCase()} = ${colorDecl};`
  );

var package = "me.barend.generateavatar";
var preamble = `//\n// This file was automatically generated using a script. DO NOT EDIT MANUALLY.\n//\n\npackage ${package};\n\nimport java.awt.*;\n`;
console.log(`${preamble}\nclass HTMLColor {\n${htmlColors.join("\n")}\n}\n`);
