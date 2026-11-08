package dev.involvex.kiln.core.privilege;

/**
 * Shizuku UserService contract. Runs in a privileged process (shell or root).
 * destroy() transaction code is required by the Shizuku server — do not change.
 */
interface IKilnUserService {
    void destroy() = 16777114;

    /**
     * Runs an allowlisted command. [commandId] must match ShellAllowlist ids;
     * [args] are validated server-side (never shell-concatenated).
     * Returns stdout+stderr or an error prefix "ERR:…".
     */
    String runAllowlisted(String commandId, in String[] args) = 1;
}
