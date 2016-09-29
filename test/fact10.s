	number	r8, 0
	number	r1, 10
	call	r7, fact
	call	r7, put
	exit
fact:	number  r2, 1
	eq	r3, r1, r2
	jmpf	r3, rec
	ret	r7
rec:	add	r8, r8, r2
	store	r8, r1
	add	r8, r8, r2
	store	r8, r7
	sub	r1, r1, r2
	call	r7, fact
	number	r2, 1
	load	r8, r7
	sub	r8, r8, r2
	load	r8, r3
	sub	r8, r8, r2
	mul	r1, r1, r3
	ret	r7
