# Check
A simple inline testing library.

Enable by setting `:me.raystubbs.check/enabled true` in the
deps aliases for which you want it to be enabled.

```clojure
:aliases {:dev {:me.raystubbs.check/enabled true}}
```

When disabled, the `check` macro expands to `nil`, so there's
no runtime cost.


```clojure
(require '[check.core :refer [check] :as ck]')

(check ::is-string?
  (assert (string? (::ck/sample ::ck/string))))
```

For each check that's executed, the reporter function is
called with its status and some other details.  The default
reporter simply prints out a stack trace on failure.

The reporter can be customized by setting the
`:me.raystubbs.check/reporter` key in your deps alias to
a the qualified symbol for a reporter function.


