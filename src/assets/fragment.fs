#version 330

in  vec2 outTexCoord;
out vec4 fragColour;

uniform sampler2D textureSampler;
uniform vec4 colour;
uniform int textured;

void main()
{
    if ( textured == 1 )
    {
        fragColour = vec4(colour);
    }
    else
    {
        fragColour = colour * texture(textureSampler, outTexCoord);
    }
}