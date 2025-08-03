#include "Global.h"

#if 0
char *alloc_sprintf(const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  char *msg = alloc_sprintf(fmt, ap);
  va_end(ap);
  return msg;
}

char *alloc_sprintf(const char *fmt, va_list ap) {

  sint4 size = 100;
  char *p;
  va_list ap2;

  if ((p = (char*)malloc (size)) == NULL) return NULL;

  for (;;) {
    /* Try to print in the allocated space. */
    va_copy(ap2, ap);
    sint4 n = vsnprintf (p, size, fmt, ap2);
    va_end(ap2);
    /* If that worked, return the string. */
    if (n > -1 && n < size)
      return p;
    /* Else try again with more space. */
    if (n > -1)    /* glibc 2.1 */
      size = n+1; /* precisely what is needed */
    else           /* glibc 2.0 */
      size *= 2;  /* twice the old size */

    if ((p = (char*)realloc (p, size)) == NULL) return NULL;
  }
}

std::ostream &form(std::ostream &os, const char *fmt, ...)
{
  va_list ap;
  va_start(ap, fmt);
  form(os, fmt, ap);
  va_end(ap);
  return os;
}

std::ostream &form(std::ostream &os, const char *fmt, va_list ap)
{
  char *msg = alloc_sprintf(fmt, ap);
  if (msg == 0) ERR("out of memory");
  os << msg;
  free(msg);
  return os;
}

#endif


