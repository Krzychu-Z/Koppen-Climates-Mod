#version 150

// Psychedelia: the world ripples, and every colour slides around the colour wheel,
// swirling out from the middle of the screen. Time runs 0 -> 1 once a second.

uniform sampler2D DiffuseSampler;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

// rotate a colour's hue by `angle` radians around the grey axis
vec3 hueShift(vec3 color, float angle) {
    const vec3 grey = vec3(0.57735);
    float c = cos(angle);
    return color * c + cross(grey, color) * sin(angle) + grey * dot(grey, color) * (1.0 - c);
}

void main() {
    float t = Time * 6.2831853;
    vec2 uv = texCoord;
    uv.x += sin(uv.y * 18.0 + t) * 0.004;
    uv.y += cos(uv.x * 14.0 + t) * 0.004;
    vec3 color = texture(DiffuseSampler, uv).rgb;

    vec3 shifted = hueShift(color, t + length(texCoord - 0.5) * 6.0);
    float luma = dot(shifted, vec3(0.299, 0.587, 0.114));
    vec3 vivid = mix(vec3(luma), shifted, 1.6);
    fragColor = vec4(clamp(mix(color, vivid, 0.75), 0.0, 1.0), 1.0);
}
