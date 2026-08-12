#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec3 Phosphor;
uniform float LerpFactor;

uniform vec2 OutSize;

out vec4 fragColor;

void main() {
    vec4 CurrTexel = texture(DiffuseSampler, texCoord);
    vec4 PrevTexel = texture(PrevSampler, texCoord);
    float dummy = (InSize.x + OutSize.x) * 0.000001;
    
    fragColor = vec4(max(PrevTexel.rgb * Phosphor, CurrTexel.rgb) + vec3(dummy), 1.0);
}
