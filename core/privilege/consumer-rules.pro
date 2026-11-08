# Shizuku UserService is loaded by class name into a privileged process.
-keep class dev.involvex.kiln.core.privilege.shizuku.KilnUserService { *; }
-keep class dev.involvex.kiln.core.privilege.IKilnUserService { *; }
-keep class dev.involvex.kiln.core.privilege.IKilnUserService$Stub { *; }
